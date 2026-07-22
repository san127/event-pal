package com.example.eventpal_organization.supervisor

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.InfoBills
import com.example.eventpal_organization.R
import com.example.eventpal_organization.adapter.SupervisorInfoListAdapter
import com.example.eventpal_organization.boss.BossAttachments
import com.example.eventpal_organization.boss.BossSingleEventInfo
import com.example.eventpal_organization.databinding.ActivitySupervisorInfoListBinding
import com.example.eventpal_organization.model.EventInfoMainItemModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SupervisorInfoList : AppCompatActivity() {
    private lateinit var binding: ActivitySupervisorInfoListBinding
    private var eventID: String = ""
    private val db = Firebase.firestore
    private lateinit var adapter: SupervisorInfoListAdapter
    private val infoList = mutableListOf<EventInfoMainItemModel>()
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySupervisorInfoListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventName = intent.getStringExtra("eventName").toString()

        binding.eventInfoTitle.text = eventName

        // Initialize adapter **with** click listener
        adapter = SupervisorInfoListAdapter(infoList) { documentId ->
            openEventDetails(documentId)  // Open new activity when an item is clicked
        }

        binding.eventInfoMainRecycler.layoutManager = LinearLayoutManager(this)
        binding.eventInfoMainRecycler.adapter = adapter

        // Fetch files from Firestore
        fetchEventInfo()
    }

    private fun fetchEventInfo() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventID = intent.getStringExtra("eventID").toString()

        val eventRef = db.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)

        // Fetch and set event name
        eventRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val eventName = document.getString("Event name") ?: "Unnamed Event"
                binding.eventInfoTitle.text = eventName
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load event name: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Fetch infoList items
        val collectionRef = eventRef.collection("infoList")
        collectionRef.get().addOnSuccessListener { querySnapshot ->
            infoList.clear()
            for (document in querySnapshot) {
                if (document.id == "Supervisors") continue

                val item = EventInfoMainItemModel(id = document.id)
                infoList.add(item)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEventDetails(documentId: String) {
        val intent = when (documentId) {
            "Bills" -> Intent(this, InfoBills::class.java).apply {
                putExtra("documentId", "bills")
                putExtra("eventName", eventName)
            }
            "Inspirations" -> Intent(this, BossAttachments::class.java).apply {
                putExtra("documentId", "Inspirations")
                putExtra("eventName", eventName)
            }
            else -> Intent(this, BossSingleEventInfo::class.java).apply {
                putExtra("documentId", documentId)
                putExtra("eventName", eventName)
            }
        }

        intent.putExtra("eventID", eventID)
        startActivity(intent)
    }

}
