package com.example.eventpal_organization.boss

import android.content.Intent
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
import com.example.eventpal_organization.adapter.BossEventInfoListAdapter
import com.example.eventpal_organization.databinding.ActivityBossEventInfoBinding
import com.example.eventpal_organization.model.EventInfoMainItemModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BossEventInfo : AppCompatActivity() {
    private lateinit var binding : ActivityBossEventInfoBinding
    private var eventID: String = ""
    private val firestoredb = Firebase.firestore
    private val itemList = mutableListOf<EventInfoMainItemModel>()
    private lateinit var adapter: BossEventInfoListAdapter
    private var eventName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityBossEventInfoBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        adapter = BossEventInfoListAdapter(itemList) { documentId ->
            openEventDetails(documentId)
        }

        binding.eventInfoMainRecycler.layoutManager = LinearLayoutManager(this)
        binding.eventInfoMainRecycler.adapter = adapter

        eventID = intent.getStringExtra("eventID").toString()

        fetchEventDetails(orgID,eventID)
        fetchEventInfo(orgID, eventID)

        binding.managerAddBtnBoss.setOnClickListener{
            //showManagerSelectionDialog(orgID, eventID)
            showManagerDialog()
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





    private fun fetchEventDetails(orgID: String, eventID: String) {
        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    eventName = document.getString("Event name").orEmpty()
                    val clientName = document.getString("Client name").orEmpty()
                    val phone = document.getString("Phone").orEmpty()
                    val venue = document.getString("Venue").orEmpty()
                    val date = document.getString("Date").orEmpty()
                    val time = document.getString("Time").orEmpty()
                    val budget = document.getString("Budget").orEmpty()
                    val managerID = document.getString("managerID").orEmpty()

                    binding.eventInfoTitle.text = eventName

                    if (managerID.isNotEmpty()) {
                        fetchManagerName(orgID, managerID) { managerName ->
                            val eventDetails = """
                        Client: $clientName
                        Phone: $phone
                        Venue: $venue
                        Date: $date
                        Time: $time
                        Budget: $budget
                        Managed By: ${managerName ?: "Unknown"}
                    """.trimIndent()
                            binding.eventDetails.text = eventDetails
                        }
                    } else {
                        val eventDetails = """
                    Client: $clientName
                    Phone: $phone
                    Venue: $venue
                    Date: $date
                    Time: $time
                    Budget: $budget
                    Managed By: Not Assigned
                """.trimIndent()
                        binding.eventDetails.text = eventDetails
                    }
                } else {
                    Toast.makeText(this, "Event details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch event details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchManagerName(orgID: String, managerID: String, callback: (String?) -> Unit) {
        firestoredb.collection(orgID)
            .document("employees")
            .collection("manager")
            .document(managerID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val managerName = document.getString("Name")
                    callback(managerName)
                } else {
                    callback(null) // Manager not found
                }
            }
            .addOnFailureListener {
                callback(null) // Error fetching manager
            }
    }



    private fun fetchEventInfo(orgID : String, eventID : String) {

        val collectionRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")

        collectionRef.get().addOnSuccessListener { querySnapshot ->
            itemList.clear()
            for (document in querySnapshot) {
                val item = EventInfoMainItemModel(id = document.id)
                itemList.add(item)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEventDetails(documentId: String) {
        val intent: Intent = when (documentId) {
            "Supervisors" -> {
                Intent(this, BossSupervisors::class.java).apply {
                    putExtra("documentId", "Supervisors")
                    putExtra("eventName", eventName)
                    //putExtra("boss", "boss")
                }
            }
            "Bills" -> {
                Intent(this, BossAttachments::class.java).apply {
                    putExtra("documentId","bills")
                    putExtra("eventName", eventName)
                }
            }
            "Inspirations" -> {
                Intent(this, BossAttachments::class.java).apply {
                    putExtra( "documentId","Inspirations")
                    putExtra("eventName", eventName)
                }
            }
            else -> {
                Intent(this, BossSingleEventInfo::class.java).apply {
                    putExtra("documentId", documentId)
                    putExtra("eventName", eventName)
                }
            }
        }

        intent.putExtra("eventID", eventID)
        startActivity(intent)
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
                        fetchEventDetails(orgID,eventID)

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

}