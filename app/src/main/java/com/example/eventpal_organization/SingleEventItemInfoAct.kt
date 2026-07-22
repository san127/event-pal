package com.example.eventpal_organization

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.adapter.HashMapRecyclerAdapter
import com.example.eventpal_organization.databinding.ActivitySingleEventItemInfoBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class SingleEventItemInfoAct : AppCompatActivity() , HashMapRecyclerAdapter.OnHashMapActionListener{
    private lateinit var binding: ActivitySingleEventItemInfoBinding
    private var documentID: String = ""
    private var eventID: String = ""
    val firestoredb = FirebaseFirestore.getInstance()
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySingleEventItemInfoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        documentID = intent.getStringExtra("documentId") ?: ""

        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        binding.SingleInfoItemText.text = documentID

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventID = intent.getStringExtra("eventID").toString()
        loadHashMaps()
        binding.fabAddInfoItemBtn.setOnClickListener {
            showCreateHashMapDialog()
        }

    }


    private fun createNewHashMapInFirestore(hashMapKey: String) {

        val emptyHashMap = mapOf<String, String>()  // An empty HashMap

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()
        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update(hashMapKey, emptyHashMap)  // Add the empty HashMap
            .addOnSuccessListener {
                Toast.makeText(this, "Content List '$hashMapKey' created successfully!", Toast.LENGTH_SHORT).show()
                loadHashMaps()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create Content List", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCreateHashMapDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_singleinfo_add_item, null)
        val editTextKey = dialogView.findViewById<EditText>(R.id.dialogueItemKey)
        eventID = intent.getStringExtra("eventID").toString()
        AlertDialog.Builder(this)
            .setTitle("Create New Content List")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val key = editTextKey.text.toString().trim()
                if (key.isNotBlank()) {
                    createNewHashMapInFirestore(key)
                } else {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



    private fun loadHashMaps() {

        eventID = intent.getStringExtra("eventID").toString()
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val hashMapList = document.data?.mapNotNull { entry ->
                        val value = entry.value
                        if (value is Map<*, *> && value.keys.all { it is String } && value.values.all { it is String }) {
                            entry.key to value as Map<String, String>
                        } else {
                            null
                        }
                    } ?: emptyList()

                    setupRecyclerView(hashMapList)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }



    private fun setupRecyclerView(hashMapList: List<Pair<String, Map<String, String>>>) {
        val adapter = HashMapRecyclerAdapter(this, hashMapList, this)
        binding.singIeInfoDisplayRecycler.layoutManager = LinearLayoutManager(this)
        binding.singIeInfoDisplayRecycler.adapter = adapter
    }



    override fun onEditKeyValue(hashMapKey: String, key: String, value: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_edit_hashmap, null)
        val editTextKey = dialogView.findViewById<EditText>(R.id.editTextKey)
        val editTextValue = dialogView.findViewById<EditText>(R.id.editTextValue)

        editTextKey.setText(key)
        editTextValue.setText(value)

        AlertDialog.Builder(this)
            .setTitle("Edit Title-Value")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
//                updateHashMapField(hashMapKey, editTextKey.text.toString(), editTextValue.text.toString())
                updateHashMapField(hashMapKey, key, editTextKey.text.toString(), editTextValue.text.toString())

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override  fun onAddKeyValue(hashMapKey: String) {
        showEditHashMapDialog(hashMapKey)
    }

    override fun onDeleteKeyValue(hashMapKey: String, key: String) {
        val firestoredb = FirebaseFirestore.getInstance()
        val orgID = getSharedPreferences("organizationID", MODE_PRIVATE).getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update("$hashMapKey.$key", FieldValue.delete())
            .addOnSuccessListener { loadHashMaps() }
    }

    override fun onDeleteHashMap(hashMapKey: String) {
        val firestoredb = FirebaseFirestore.getInstance()
        val orgID = getSharedPreferences("organizationID", MODE_PRIVATE).getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update(hashMapKey, FieldValue.delete())
            .addOnSuccessListener { loadHashMaps() }
    }

    private fun updateHashMapField(hashMapKey: String, oldKey: String, newKey: String, newValue: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        val docRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)

        // First: Delete the old key
        val updates = hashMapOf<String, Any>(
            "$hashMapKey.$oldKey" to FieldValue.delete(),
            "$hashMapKey.$newKey" to newValue
        )

        docRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Key updated successfully!", Toast.LENGTH_SHORT).show()
                loadHashMaps()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update key", Toast.LENGTH_SHORT).show()
            }
    }



//    private fun updateHashMapField(hashMapKey: String, key: String, newValue: String) {
//        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
//        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
//        eventID = intent.getStringExtra("eventID").toString()
//
//        firestoredb.collection(orgID)
//            .document("Events")
//            .collection("ManagerAssignedEvents")
//            .document(eventID)
//            .collection("infoList")
//            .document(documentID)
//            .update("$hashMapKey.$key", newValue)  // Update Firestore field
//            .addOnSuccessListener {
//                Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
//                loadHashMaps()  // Refresh UI
//            }
//            .addOnFailureListener {
//                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
//            }
//    }



    private fun showEditHashMapDialog(hashMapKey: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialogue_edit_hashmap, null)
        val editTextKey = dialogView.findViewById<EditText>(R.id.editTextKey)
        val editTextValue = dialogView.findViewById<EditText>(R.id.editTextValue)

        AlertDialog.Builder(this)
            .setTitle("Edit : $hashMapKey")
            .setView(dialogView)
            .setPositiveButton("Add Key-Value") { _, _ ->
                val key = editTextKey.text.toString().trim()
                val value = editTextValue.text.toString().trim()
                if (key.isNotBlank() && value.isNotBlank()) {
                    addKeyValueToHashMap(hashMapKey, key, value)
                } else {
                    Toast.makeText(this, "Title and Value cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                deleteHashMap(hashMapKey)  // Call delete function
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun updateHashMapValue(hashMapKey: String, key: String, newValue: String) {

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update("$hashMapKey.$key", newValue)
            .addOnSuccessListener {
                Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show()
                loadHashMaps()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show()
            }
    }



    private fun addKeyValueToHashMap(hashMapKey: String, key: String, value: String) {
        eventID = intent.getStringExtra("eventID").toString()
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update("$hashMapKey.$key", value)  // Nested update for key-value pair
            .addOnSuccessListener {
                Toast.makeText(this, "Key-Value pair added successfully!", Toast.LENGTH_SHORT).show()
                loadHashMaps()  // Refresh RecyclerView
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add key-value pair", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteHashMap(hashMapKey: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update(hashMapKey, FieldValue.delete())  // Deletes the entire hashmap
            .addOnSuccessListener {
                Toast.makeText(this, "HashMap deleted successfully!", Toast.LENGTH_SHORT).show()
                loadHashMaps()  // Refresh RecyclerView
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete HashMap", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteKeyValuePair(hashMapKey: String, key: String) {
        val firestoredb = FirebaseFirestore.getInstance()
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document(documentID)
            .update("$hashMapKey.$key", FieldValue.delete())  // Deletes the key-value pair
            .addOnSuccessListener {
                Toast.makeText(this, "Key-Value pair deleted!", Toast.LENGTH_SHORT).show()
                loadHashMaps()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete Key-Value pair", Toast.LENGTH_SHORT).show()
            }
    }

}