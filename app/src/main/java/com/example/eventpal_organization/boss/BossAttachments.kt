package com.example.eventpal_organization.boss

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
import com.example.eventpal_organization.databinding.ActivityBossAttachmentsBinding
import com.google.firebase.storage.FirebaseStorage

class BossAttachments : AppCompatActivity() {
    private lateinit var binding: ActivityBossAttachmentsBinding
    private var documentID: String = ""
    private var eventID: String = ""
    private lateinit var adapter: BossAttachmentsAdapter
    private val fileList = mutableListOf<StorageFile>()
    private lateinit var storageRef: com.google.firebase.storage.StorageReference
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBossAttachmentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        eventName = intent.getStringExtra("eventName").toString()

        documentID = intent.getStringExtra("documentId") ?: ""
        eventID = intent.getStringExtra("eventID") ?: ""

        binding.eventInfoTitle.text = eventName

        if (documentID=="Inspirations"){
            binding.InfoItemText.text = "Inspirations"
        } else{
            binding.InfoItemText.text = documentID.capitalize()
        }




        // Set Firebase Storage reference based on documentID
        storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/$documentID")

        // Initialize adapter with empty list
        adapter = BossAttachmentsAdapter(this, fileList)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        // Fetch files from Firebase Storage
        fetchFiles(storageRef)
    }

    private fun fetchFiles(storageRef: com.google.firebase.storage.StorageReference) {

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

//    private fun fetchFiles(storageRef: com.google.firebase.storage.StorageReference) {
//        storageRef.listAll()
//            .addOnSuccessListener { result ->
//                fileList.clear()
//                val tempList = mutableListOf<StorageFile>()
//
//                val validFiles = result.items.filter { !it.name.startsWith("placeholder", ignoreCase = true) }
//
//                if (validFiles.isEmpty()) {
//                    adapter.notifyDataSetChanged()
//                    return@addOnSuccessListener
//                }
//
//                for (fileRef in validFiles) {
//                    fileRef.downloadUrl.addOnSuccessListener { uri ->
//                        val fileName = fileRef.name
//                        val fileType = fileName.substringAfterLast('.', "").lowercase()
//
//                        val thumbnailUrl = when (fileType) {
//                            "jpg", "jpeg", "png" -> uri.toString() // Show actual image
//                            "pdf" -> "https://firebasestorage.googleapis.com/v0/b/YOUR_BUCKET/o/pdf_icon.png?alt=media"
//                            else -> "https://firebasestorage.googleapis.com/v0/b/YOUR_BUCKET/o/default_file_icon.png?alt=media"
//                        }
//
//                        tempList.add(StorageFile(fileName, uri.toString(), thumbnailUrl))
//
//                        if (tempList.size == validFiles.size) {
//                            fileList.addAll(tempList)
//                            adapter.notifyDataSetChanged()
//                        }
//                    }
//                }
//            }
//            .addOnFailureListener { e ->
//                Toast.makeText(this, "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
//            }
//    }
}
