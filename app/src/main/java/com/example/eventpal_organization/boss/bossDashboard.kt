package com.example.eventpal_organization.boss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.CompletedEvents
import com.example.eventpal_organization.OrganizationInput
import com.example.eventpal_organization.R
import com.example.eventpal_organization.UserProfile
import com.example.eventpal_organization.adapter.BossClientEventsRecyclerAdapter
import com.example.eventpal_organization.adapter.BossEventsRecyclerAdapter
import com.example.eventpal_organization.databinding.ActivityBossDashboardBinding
import com.example.eventpal_organization.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class bossDashboard : AppCompatActivity() {
    private lateinit var binding : ActivityBossDashboardBinding
    private val firestoredb = Firebase.firestore
    private lateinit var bossEventsAdapter: BossEventsRecyclerAdapter
    private lateinit var bossClientEventsAdapter: BossClientEventsRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBossDashboardBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        fetchUserName(orgID)

        setupRecyclerViews()

        // Fetch Events and Client Events
        fetchEvents(orgID)
        fetchClientEvents(orgID)
        listenForEventUpdates(orgID)

        binding.hamburger.setOnClickListener { view ->
            showMenu(view)
        }

        binding.notifImg.setOnClickListener{
            startActivity(Intent(this, CompletedEvents::class.java))
        }
    }

    private fun fetchUserName(orgID : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userID = currentUser.uid

        firestoredb.collection(orgID)
            .document("employees")
            .collection("boss")
            .document(userID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("Name") ?: "User"
                    val firstName = fullName.split(" ").firstOrNull() ?: fullName
                    binding.helloText.text = "Hello, $firstName"
                } else {
                    binding.helloText.text = "Hello, User"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load user name", Toast.LENGTH_SHORT).show()
            }
    }


    private fun logout() {

        FirebaseAuth.getInstance().signOut()

        // clear shared preferences
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        val org = getSharedPreferences("organizationID", MODE_PRIVATE)
        val editShared = org.edit()
        editShared.clear()
        editShared.apply()

        // take to organization input activity
        val intent = Intent(this, OrganizationInput::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerViews() {
        // Setup RecyclerView for Events assigned by managers
        bossEventsAdapter = BossEventsRecyclerAdapter(emptyList()) { eventID ->
            val intent = Intent(this, BossEventInfo::class.java)
            intent.putExtra("eventID", eventID)  // Pass eventId to BossEventInfo
            startActivity(intent)
        }
        binding.eventsRecycler.apply {
            layoutManager = GridLayoutManager(this@bossDashboard, 2)
            adapter = bossEventsAdapter
        }

        // Setup RecyclerView for Client Events
        bossClientEventsAdapter = BossClientEventsRecyclerAdapter(emptyList()) { eventID ->
            val intent = Intent(this, BossClientEventInfo::class.java) // Open ClientEventInfo activity
            intent.putExtra("eventID", eventID)
            startActivity(intent)
        }
        binding.clientEventsRecycler.apply {
            layoutManager = GridLayoutManager(this@bossDashboard, 2)
            adapter = bossClientEventsAdapter
        }
    }



    private fun fetchEvents(orgID : String) {
    firestoredb.collection(orgID)
        .document("Events")
        .collection("ManagerAssignedEvents").get()
        .addOnSuccessListener { documents ->
            val eventsList = documents.map { doc ->
                Event(
                    eventId = doc.id,
                    eventName = doc.getString("Event name") ?: "No Name",
                    eventDate = doc.getString("Date") ?: "No Date"
                )
            }
            bossEventsAdapter.updateEvents(eventsList)
        }
        .addOnFailureListener {
            bossEventsAdapter.updateEvents(emptyList())  // Show empty list if failed
        }
}

private fun fetchClientEvents(orgID : String) {
    firestoredb.collection(orgID)
        .document("Events")
        .collection("ClientAddedEvents").get()
        .addOnSuccessListener { documents ->
            val clientEventsList = documents.map { doc ->
                Event(
                    eventId = doc.id,
                    eventName = doc.getString("Event name") ?: "No Name",
                    eventDate = doc.getString("Date") ?: "No Date"
                )
            }
            bossClientEventsAdapter.updateClientEvents(clientEventsList)
        }
        .addOnFailureListener {
            bossClientEventsAdapter.updateClientEvents(emptyList())  // Show empty list if failed
        }
}

    private fun listenForEventUpdates(orgID: String) {
        val managerEventsRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")

        val clientEventsRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ClientAddedEvents")

        // Listen for Manager Assigned Events
        managerEventsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error fetching updates: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val eventsList = snapshots.documents.map { doc ->
                    Event(
                        eventId = doc.id,
                        eventName = doc.getString("Event name") ?: "No Name",
                        eventDate = doc.getString("Date") ?: "No Date"
                    )
                }
                bossEventsAdapter.updateEvents(eventsList)
            }
        }

        // Listen for Client Added Events
        clientEventsRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                Toast.makeText(this, "Error fetching updates: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshots != null) {
                val clientEventsList = snapshots.documents.map { doc ->
                    Event(
                        eventId = doc.id,
                        eventName = doc.getString("Event name") ?: "No Name",
                        eventDate = doc.getString("Date") ?: "No Date"
                    )
                }
                bossClientEventsAdapter.updateClientEvents(clientEventsList)
            }
        }
    }

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.dashboard_menu, popupMenu.menu) // Updated reference
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    logout()
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this, UserProfile::class.java))
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }




}