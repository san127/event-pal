package com.example.eventpal_organization

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.adapter.BossEventInfoListAdapter
import com.example.eventpal_organization.boss.BossAttachments
import com.example.eventpal_organization.boss.BossSingleEventInfo
import com.example.eventpal_organization.boss.BossSupervisors
import com.example.eventpal_organization.databinding.ActivityBossEventInfoBinding
import com.example.eventpal_organization.databinding.ActivityCompletedEventInfoBinding
import com.example.eventpal_organization.databinding.ActivityCompletedEventsBinding
import com.example.eventpal_organization.model.EventInfoMainItemModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CompletedEventInfo : AppCompatActivity() {
    private lateinit var binding : ActivityCompletedEventInfoBinding
    private var eventID: String = ""
    private val firestoredb = Firebase.firestore
    private val itemList = mutableListOf<EventInfoMainItemModel>()
    private lateinit var adapter: BossEventInfoListAdapter
    private var eventName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityCompletedEventInfoBinding.inflate(layoutInflater)
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

    }


    private fun fetchEventDetails(orgID: String, eventID: String) {
        firestoredb.collection(orgID)
            .document("Events")
            .collection("CompletedEvents")
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
            .collection("CompletedEvents")
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

}