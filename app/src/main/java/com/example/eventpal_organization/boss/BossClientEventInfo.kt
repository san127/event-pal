package com.example.eventpal_organization.boss

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eventpal_organization.R
import com.example.eventpal_organization.StorageFile
import com.example.eventpal_organization.databinding.ActivityBossClientEventInfoBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class BossClientEventInfo : AppCompatActivity() {
    private lateinit var binding : ActivityBossClientEventInfoBinding
    private var eventID: String = ""
    private val firestoredb = Firebase.firestore
    private lateinit var adapter: BossAttachmentsAdapter
    private val fileList = mutableListOf<StorageFile>()
    //private lateinit var storageRef: com.google.firebase.storage.StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBossClientEventInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        fetchEventName(orgID)

        if (eventID.isNotEmpty()) {
            fetchEventDetails()
            //fetchFiles()
        } else {
            Toast.makeText(this, "No Event ID found", Toast.LENGTH_SHORT).show()
        }

        adapter = BossAttachmentsAdapter(this, fileList)
        binding.eventInfoMainRecycler.layoutManager = GridLayoutManager(this,2)
        binding.eventInfoMainRecycler.adapter = adapter

       fetchFiles()



        binding.rejectBtn.setOnClickListener{
            rejectEvent(orgID)
        }

        binding.acceptBtn.setOnClickListener{
            acceptEvent(orgID)
        }


    }

    private fun fetchFiles() {
        val storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/Inspirations")
        storageRef.listAll()
            .addOnSuccessListener { result ->
                fileList.clear()

                val validFiles = result.items.filter { fileRef ->
                    fileRef.name.lowercase() != "placeholder" && !fileRef.name.lowercase().startsWith("placeholder")
                }

                if (validFiles.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (fileRef in validFiles) {
                    fileRef.metadata.addOnSuccessListener { metadata ->
                        // Ensure file is not empty
                        if (metadata.sizeBytes > 0) {
                            fileRef.downloadUrl.addOnSuccessListener { uri ->
                                val fileName = fileRef.name
                                fileList.add(StorageFile(fileName, uri.toString(), ""))

                                // Notify adapter each time a new file is added
                                adapter.notifyItemInserted(fileList.size - 1)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




//    private fun fetchFiles() {
//        storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/Inspirations")
//        storageRef.listAll()
//            .addOnSuccessListener { result ->
//                fileList.clear()
//
//                val validFiles = result.items.filter { fileRef ->
//                    fileRef.name.lowercase() != "placeholder" && !fileRef.name.lowercase().startsWith("placeholder")
//                }
//
//                if (validFiles.isEmpty()) {
//                    adapter.notifyDataSetChanged()
//                    return@addOnSuccessListener
//                }
//
//                for (fileRef in validFiles) {
//                    fileRef.metadata.addOnSuccessListener { metadata ->
//                        // Ensure file is not empty
//                        if (metadata.sizeBytes > 0) {
//                            fileRef.downloadUrl.addOnSuccessListener { uri ->
//                                val fileName = fileRef.name
//                                fileList.add(StorageFile(fileName, uri.toString(), ""))
//
//                                // Notify adapter each time a new file is added
//                                adapter.notifyItemInserted(fileList.size - 1)
//                            }
//                        }
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }



    private fun fetchEventDetails() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ClientAddedEvents")
            .document(eventID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val eventName = document.getString("Event name").orEmpty()
                    val clientName = document.getString("Client name").orEmpty()
                    val phone = document.getString("Phone").orEmpty()
                    val venue = document.getString("Venue").orEmpty()
                    val date = document.getString("Date").orEmpty()
                    val time = document.getString("Time").orEmpty()
                    val budget = document.getString("Budget").orEmpty()

                    binding.eventInfoTitle.text = eventName

                    val eventDetails = """
                    Client: $clientName
                    Phone: $phone
                    Venue: $venue
                    Date: $date
                    Time: $time
                    Budget: $budget
                """.trimIndent()

                    binding.eventDetails.text = eventDetails
                } else {
                    Toast.makeText(this, "Event details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch event details: ${e.message}", Toast.LENGTH_SHORT).show()
            }


    }




    private fun fetchEventName(orgID: String) {
        eventID = intent.getStringExtra("eventID").toString()

        val eventRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ClientAddedEvents") // Change this if needed to ManagerAssignedEvents
            .document(eventID)

        eventRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val eventName = document.getString("Event name") ?: "Unnamed Event"
                binding.eventInfoTitle.text = eventName // Set the event name in UI
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load event name: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun acceptEvent(orgID: String) {
       // val firestore = FirebaseFirestore.getInstance()
        eventID = intent.getStringExtra("eventID").toString()

        val clientAddedEventsRef = firestoredb.collection(orgID).document("Events").collection("ClientAddedEvents").document(eventID)
        val managerAssignedEventsRef = firestoredb.collection(orgID).document("Events").collection("ManagerAssignedEvents").document(eventID)

        clientAddedEventsRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Copy data to new collection
                managerAssignedEventsRef.set(document.data!!)
                    .addOnSuccessListener {
                        // Delete the document from the old collection
                        clientAddedEventsRef.delete()
                            .addOnSuccessListener {
                                // Start new activity after moving the document
                                createStorageFolders(eventID)
                                createInfoList(orgID,eventID)
                                val intent = Intent(this, BossEventInfo::class.java)
                                intent.putExtra("eventID", eventID)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to delete event: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to move event: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error fetching event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createStorageFolders(eventId: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val folders = listOf("bills","Inspirations")

        for (folder in folders) {
            val folderRef = storageRef.child("events/$eventId/$folder/placeholder.txt")
            val placeholderData = "This is a placeholder file to keep the folder".toByteArray()

            folderRef.putBytes(placeholderData)
                .addOnSuccessListener {
                   // Toast.makeText(this, "Created $folder folder in Storage", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    //Toast.makeText(this, "Failed to create $folder folder", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun createInfoList(orgID: String, eventId: String) {
        val infoList = listOf("Bills", "Inspirations")
        val eventDocRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventId)
            .collection("infoList")

        for (info in infoList) {
            eventDocRef.document(info).set(emptyMap<String, Any>()) // Create an empty document
                .addOnFailureListener { e ->
                   // Toast.makeText(this, "Failed to create $info: ${e.message}", Toast.LENGTH_SHORT) .show()
                }
        }
    }

    private fun rejectEvent(orgID: String) {
        val eventID = intent.getStringExtra("eventID").toString()

        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Reject Event")
        builder.setMessage("Are you sure you want to reject this event? This action cannot be undone")

        builder.setPositiveButton("Yes") { _, _ ->
            val eventRef = firestoredb.collection(orgID)
                .document("Events")
                .collection("ClientAddedEvents")
                .document(eventID)

            eventRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Event rejected successfully!", Toast.LENGTH_SHORT).show()
                    finish() // Close the activity after deletion
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to reject event: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss() // Close the dialog
        }

        builder.create().show()
    }

}