package com.example.eventpal_organization

import RecceAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.eventpal_organization.databinding.ActivityInfoBillsBinding
import com.google.firebase.storage.FirebaseStorage

class InfoBills : AppCompatActivity() {
    private var eventID: String = ""
    private lateinit var binding : ActivityInfoBillsBinding
    private lateinit var adapter: RecceAdapter
    private val fileList = mutableListOf<StorageFile>()
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInfoBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventID = intent.getStringExtra("eventID").toString()
        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        val storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/bills/")

        // Set up RecyclerView
        adapter = RecceAdapter(fileList, this::deleteFile, this)
        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

        // Button to select and upload files
        binding.addRecceBtn.setOnClickListener {
            selectFile()
        }

        // Fetch existing files
        fetchFiles(storageRef)
    }

    // File picker
    private val filePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { showRenameDialog(it) }
    }

    private fun selectFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        filePicker.launch(intent.type!!)
    }

    private fun showRenameDialog(fileUri: Uri) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rename_file, null)
        val editText = dialogView.findViewById<EditText>(R.id.editTextFileName)

        AlertDialog.Builder(this)
            .setTitle("Rename File")
            .setView(dialogView)
            .setPositiveButton("Upload") { _, _ ->
                val fileName = editText.text.toString().trim()
                if (fileName.isNotEmpty()) uploadFile(fileUri, fileName) else Toast.makeText(this, "Enter a file name", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadFile(fileUri: Uri, fileName: String) {
        val storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/bills/")
        val ref = storageRef.child("$fileName.${contentResolver.getType(fileUri)?.split("/")?.last()}")

        ref.putFile(fileUri)
            .addOnSuccessListener {
                Toast.makeText(this, "File Uploaded", Toast.LENGTH_SHORT).show()
                fetchFiles(storageRef)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFiles(storageRef: com.google.firebase.storage.StorageReference) {
        storageRef.listAll()
            .addOnSuccessListener { result ->
                fileList.clear()
                val tempList = mutableListOf<StorageFile>()

                val validFiles = result.items.filter { !it.name.startsWith("placeholder", ignoreCase = true) }

                if (validFiles.isEmpty()) {
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                for (fileRef in validFiles) {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        val fileName = fileRef.name
                        val fileType = fileName.substringAfterLast('.', "").lowercase()

                        val thumbnailUrl = when (fileType) {
                            "jpg", "jpeg", "png" -> uri.toString() // Show actual image
                            "pdf" -> "https://firebasestorage.googleapis.com/v0/b/YOUR_BUCKET/o/pdf_icon.png?alt=media"
                            else -> "https://firebasestorage.googleapis.com/v0/b/YOUR_BUCKET/o/default_file_icon.png?alt=media"
                        }

                        tempList.add(StorageFile(fileName, uri.toString(), thumbnailUrl))

                        if (tempList.size == validFiles.size) {
                            fileList.addAll(tempList)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load files: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun deleteFile(file: StorageFile) {
        val storageRef = FirebaseStorage.getInstance().reference.child("events/$eventID/bills/")
        storageRef.child(file.name).delete()
            .addOnSuccessListener {
                fileList.remove(file)
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "File Deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}