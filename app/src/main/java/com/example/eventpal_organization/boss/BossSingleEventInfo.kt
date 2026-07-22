package com.example.eventpal_organization.boss

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.R
import com.example.eventpal_organization.adapter.BossHashMapRecyclerAdapter
import com.example.eventpal_organization.databinding.ActivityBossSingleEventInfoBinding
import com.google.firebase.firestore.FirebaseFirestore

class BossSingleEventInfo : AppCompatActivity() {
    private lateinit var binding : ActivityBossSingleEventInfoBinding
    private var documentID: String = ""
    private var eventID: String = ""
    val firestoredb = FirebaseFirestore.getInstance()
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBossSingleEventInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        documentID = intent.getStringExtra("documentId") ?: ""
        eventID = intent.getStringExtra("eventID") ?: ""

        binding.SingleInfoItemText.text = documentID

        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        setupRecyclerView()
        loadHashMaps()
    }

    private fun setupRecyclerView() {
        binding.singIeInfoDisplayRecycler.layoutManager = LinearLayoutManager(this)
    }

    private fun loadHashMaps() {
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

                    binding.singIeInfoDisplayRecycler.adapter = BossHashMapRecyclerAdapter(this, hashMapList)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }
}