package com.example.eventpal_organization.accountant

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.R
import com.example.eventpal_organization.StorageFile
import com.example.eventpal_organization.boss.BossAttachmentsAdapter

import com.example.eventpal_organization.databinding.ActivityAccountantBillsInfoBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage

class AccountantBillsInfo : AppCompatActivity() {
    private var eventID: String = ""
    private lateinit var binding : ActivityAccountantBillsInfoBinding
    private val firestoredb = Firebase.firestore
    private lateinit var adapter: BossAttachmentsAdapter
    private val fileList = mutableListOf<StorageFile>()
    private lateinit var storageRef: com.google.firebase.storage.StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAccountantBillsInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchEventDetails()


        adapter =BossAttachmentsAdapter(this, fileList)
        binding.eventInfoMainRecycler.layoutManager = GridLayoutManager(this,2)
        binding.eventInfoMainRecycler.adapter = adapter

        fetchFiles()

    }

    private fun fetchEventDetails() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
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

    private fun fetchFiles() {
        storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/bills")
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



}