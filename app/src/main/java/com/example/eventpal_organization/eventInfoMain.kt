package com.example.eventpal_organization

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.adapter.AdaptEvenInfoMainRecycler
import com.example.eventpal_organization.boss.BossAttachments
import com.example.eventpal_organization.databinding.ActivityEventInfoMainBinding
import com.example.eventpal_organization.model.EventInfoMainItemModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class eventInfoMain : AppCompatActivity() {
    private lateinit var binding: ActivityEventInfoMainBinding
    private val firestoredb = Firebase.firestore
    private val itemList = mutableListOf<EventInfoMainItemModel>()
    private lateinit var adapter: AdaptEvenInfoMainRecycler
    private var eventID: String = ""
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventInfoMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        adapter = AdaptEvenInfoMainRecycler(itemList) { documentId ->
            openEventDetails(documentId)
        }

        binding.eventInfoMainRecycler.layoutManager = LinearLayoutManager(this)
        binding.eventInfoMainRecycler.adapter = adapter

        fetchEventInfo()


        binding.fabAddInfoItemBtn.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun fetchEventInfo() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventID = intent.getStringExtra("eventID").toString()

        // Reference to the Event document
        val eventRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)

        // Fetch the Event name
        eventRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val eventName = document.getString("Event name") ?: "Unnamed Event"
                binding.eventInfoTitle.text = eventName  // Set text in UI
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load event name: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        // Fetch infoList items
        val collectionRef = eventRef.collection("infoList")
        collectionRef.get().addOnSuccessListener { querySnapshot ->
            itemList.clear()
            for (document in querySnapshot) {
                if (document.id == "Supervisors") continue

                val item = EventInfoMainItemModel(id = document.id)
                itemList.add(item)
            }
            adapter.notifyDataSetChanged()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showAddItemDialog() {
        // dialog for adding a new document to fire store
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add Item")
            .setView(dialogView)
            .setPositiveButton("Add") { dialog, _ ->
                val itemTitle = dialogView.findViewById<android.widget.EditText>(R.id.dialogueItemContent).text.toString()
                if (itemTitle.isNotEmpty()) {
                    addItemToFirestore(itemTitle)
                } else {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        dialogBuilder.create().show()
    }

    private fun addItemToFirestore(title: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val collectionRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")

        collectionRef.document(title).set(hashMapOf("placeholder" to true))
            .addOnSuccessListener {
                itemList.add(EventInfoMainItemModel(id = title))
                adapter.notifyItemInserted(itemList.size - 1)
                Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add item: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openEventDetails(documentId: String) {
        val intent = when (documentId) {
            "Bills" -> Intent(this, InfoBills::class.java).apply {
                putExtra("documentId", "bills")
                putExtra("eventName", eventName)
            }
            "Supervisors" -> Intent(this, EventSupervisors::class.java).apply {
                putExtra("documentId", "Supervisors")
                putExtra("eventName", eventName)
            }
            "Inspirations" -> Intent(this, BossAttachments::class.java).apply {
                putExtra("documentId", "Inspirations")
                putExtra("eventName", eventName)
            }
            else -> Intent(this, SingleEventItemInfoAct::class.java).apply {
                putExtra("documentId", documentId)
                putExtra("eventName", eventName)
            }
        }

        intent.putExtra("eventID", eventID)
        startActivity(intent)
    }



    // Pass eventID to the next activity

}