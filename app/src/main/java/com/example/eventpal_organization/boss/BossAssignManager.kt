package com.example.eventpal_organization.boss

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.R
import com.example.eventpal_organization.adapter.SupervisorAdapter
import com.example.eventpal_organization.databinding.ActivityBossAssignManagerBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BossAssignManager : AppCompatActivity() {
    private lateinit var binding: ActivityBossAssignManagerBinding
    private val firestoredb = Firebase.firestore
    private lateinit var adapter: SupervisorAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBossAssignManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        adapter = SupervisorAdapter(mutableListOf()) { supervisorName ->
            deleteManager()
        }

        binding.singIeInfoDisplayRecycler.adapter = adapter
        binding.singIeInfoDisplayRecycler.layoutManager = LinearLayoutManager(this)

        binding.fabAddManagerBtn.setOnClickListener {
            showManagerDialog()
        }
    }

    private fun deleteManager() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .update("managerID", null) // Remove assigned manager
            .addOnSuccessListener {
                Toast.makeText(this, "Manager removed successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to remove manager: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showManagerDialog() {
        val managerList = mutableListOf<DocumentSnapshot>()
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("employees")
            .collection("manager")
            .get()
            .addOnSuccessListener { documents ->
                managerList.clear()
                managerList.addAll(documents)

                val managerNames = managerList.map { it.getString("Name").orEmpty() }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Select Manager")
                    .setItems(managerNames) { _, which ->
                        val selectedManager = managerList[which]
                        addManagerToFirestore(selectedManager)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch managers", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addManagerToFirestore(managerDoc: DocumentSnapshot) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        val managerUID = managerDoc.id

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .get()
            .addOnSuccessListener { eventSnapshot ->
                val eventName = eventSnapshot.getString("Event name").orEmpty()
                val eventDate = eventSnapshot.getString("Date").orEmpty()


                val eventRef = firestoredb.collection(orgID)
                    .document("Events")
                    .collection("ManagerAssignedEvents")
                    .document(eventID)

                eventRef.update("managerID", managerUID)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Manager assigned successfully", Toast.LENGTH_SHORT)
                            .show()
                        addNotificationForManager(managerDoc.id, eventName, eventDate)

                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                            this,
                            "Failed to assign manager: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

        private fun addNotificationForManager(
            managerUID: String,
            eventName: String,
            eventDate: String
        ) {
            val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
            val orgID = sharedPreferences.getString("orgID", "").orEmpty()

            val notificationData = hashMapOf(
                "title" to "New Event Assigned",
                "message" to "$eventName is scheduled for $eventDate",
                "timestamp" to com.google.firebase.Timestamp.now() // Firestore-native Timestamp
            )

            val managerNotifRef = firestoredb.collection(orgID)
                .document("employees")
                .collection("manager")
                .document(managerUID)
                .collection("Notifications")

            managerNotifRef.add(notificationData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Notification added successfully for manager: $managerUID")

                    // Check if notifications exceed 10, and delete the oldest
                    managerNotifRef.orderBy(
                        "timestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING
                    )
                        .get()
                        .addOnSuccessListener { documents ->
                            val notificationsToDelete =
                                documents.documents.drop(10) // Keep only the latest 10

                            for (doc in notificationsToDelete) {
                                managerNotifRef.document(doc.id).delete()
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
                    Log.e(
                        "Firestore",
                        "Failed to add notification for manager: ${exception.message}"
                    )
                }
        }

    }
