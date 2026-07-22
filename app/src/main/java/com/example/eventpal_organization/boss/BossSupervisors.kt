package com.example.eventpal_organization.boss

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.R
import com.example.eventpal_organization.adapter.BossSupervisorAdapter
import com.example.eventpal_organization.adapter.SupervisorAdapter
import com.example.eventpal_organization.databinding.ActivityBossSupervisorsBinding
import com.example.eventpal_organization.databinding.ActivityEventSupervisorsBinding
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class BossSupervisors : AppCompatActivity() {
    private lateinit var binding : ActivityBossSupervisorsBinding
    private val firestoredb = Firebase.firestore
    private lateinit var adapter: BossSupervisorAdapter
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBossSupervisorsBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventName = intent.getStringExtra("eventName").toString()
        binding.eventInfoTitle.text = eventName

        adapter = BossSupervisorAdapter(mutableListOf())

        binding.singIeInfoDisplayRecycler.adapter = adapter
        binding.singIeInfoDisplayRecycler.layoutManager = LinearLayoutManager(this)

        listenForSupervisorUpdates()


    }




    private fun listenForSupervisorUpdates() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("infoList")
            .document("Supervisors")
            .addSnapshotListener { documentSnapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to fetch updates", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val supervisorList = documentSnapshot?.data?.map { (key, value) ->
                    val phone = (value as? Map<*, *>)?.get("Phone") as? String ?: "N/A"
                    key to phone
                } ?: emptyList()

                adapter.updateList(supervisorList)
            }
    }

}

