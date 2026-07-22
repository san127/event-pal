package com.example.eventpal_organization

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.adapter.SupervisorAdapter
import com.example.eventpal_organization.databinding.ActivityEventSupervisorsBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EventSupervisors : AppCompatActivity() {
    private lateinit var binding : ActivityEventSupervisorsBinding
    private val firestoredb = Firebase.firestore
    private lateinit var adapter: SupervisorAdapter
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventSupervisorsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bossAct =  intent.getStringExtra("boss").toString()

        if(bossAct == "boss"){
            binding.fabAddSuperBtn.visibility = View.INVISIBLE
        }

        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        adapter = SupervisorAdapter(mutableListOf()) { supervisorName ->
            deleteSupervisor(supervisorName)
        }

        binding.singIeInfoDisplayRecycler.adapter = adapter
        binding.singIeInfoDisplayRecycler.layoutManager = LinearLayoutManager(this)

        listenForSupervisorUpdates()


        binding.fabAddSuperBtn.setOnClickListener{
            showSupervisorDialog()
        }


    }

    private fun deleteSupervisor(supervisorName: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document("Supervisors")
            .update(mapOf(supervisorName to FieldValue.delete()))
            .addOnSuccessListener {
                Toast.makeText(this, "$supervisorName Removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to remove $supervisorName: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun listenForSupervisorUpdates() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document("Supervisors")
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to fetch updates", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val supervisorList = documentSnapshot?.data?.map { (key, value) ->
                    val phone = (value as? Map<*, *>)?.get("Phone") as? String ?: "N/A"
                    key to phone
                } ?: emptyList()

                adapter.updateList(supervisorList)
            }
    }


    private fun showSupervisorDialog() {
        val supervisorList = mutableListOf<DocumentSnapshot>()

        // Fetch supervisor list from Firestore
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("employees")
            .collection("supervisor")
            .get()
            .addOnSuccessListener { documents ->
                supervisorList.clear()
                supervisorList.addAll(documents)

                // Show the dialog with supervisor names
                val supervisorNames = supervisorList.map { it.getString("Name").orEmpty() }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Select Supervisor")
                    .setItems(supervisorNames) { _, which ->
                        // On item selected
                        val selectedSupervisor = supervisorList[which]
                        addSupervisorToFirestore(selectedSupervisor)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch supervisors", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addSupervisorToFirestore(supervisorDoc: DocumentSnapshot) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        val supervisorName = supervisorDoc.getString("Name").orEmpty()
        val supervisorPhone = supervisorDoc.getString("Phone").orEmpty()
        val supervisorUID = supervisorDoc.id  // Assuming UID is stored as the document ID

        if (supervisorName.isEmpty() || supervisorPhone.isEmpty()) {
            Toast.makeText(this, "Invalid supervisor data", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch event details
        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .get()
            .addOnSuccessListener { eventSnapshot ->
                val eventName = eventSnapshot.getString("Event name").orEmpty()
                val eventDate = eventSnapshot.getString("Date").orEmpty()

                if (eventName.isEmpty() || eventDate.isEmpty()) {
                    Toast.makeText(this, "Failed to fetch event details", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                // Add supervisor to Firestore
                val supervisorData = hashMapOf(
                    "Phone" to supervisorPhone,
                    "UID" to supervisorUID
                )

                firestoredb.collection(orgID)
                    .document("Events")
                    .collection("ManagerAssignedEvents")
                    .document(eventID)
                    .collection("infoList")
                    .document("Supervisors")
                    .set(hashMapOf(supervisorName to supervisorData), SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(this, "$supervisorName added successfully", Toast.LENGTH_SHORT).show()

                        // Now, add a notification inside the supervisor's Firestore document
                        addNotificationForSupervisor(supervisorUID, eventName, eventDate)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to add $supervisorName: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch event details", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to add notification inside the supervisor's document
    private fun addNotificationForSupervisor(supervisorUID: String, eventName: String, eventDate: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val notificationData = hashMapOf(
            "title" to "New Event Assigned",
            "message" to "$eventName is scheduled for $eventDate",
            "timestamp" to com.google.firebase.Timestamp.now() // Firestore-native Timestamp
        )

        val supervisorNotifRef = firestoredb.collection(orgID)
            .document("employees")
            .collection("supervisor")
            .document(supervisorUID)
            .collection("Notifications")

        supervisorNotifRef.add(notificationData)
            .addOnSuccessListener {
                Log.d("Firestore", "Notification added successfully for $supervisorUID")

                // Now check if notifications exceed 10, and delete the oldest
                supervisorNotifRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { documents ->
                        val notificationsToDelete = documents.documents.drop(10) // Get extra notifications

                        for (doc in notificationsToDelete) {
                            supervisorNotifRef.document(doc.id).delete()
                                .addOnSuccessListener {
                                    Log.d("Firestore", "Deleted excess notification: ${doc.id}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error deleting old notification", e)
                                }
                        }
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Failed to add notification: ${exception.message}")
            }
    }


}