package com.example.eventpal_organization

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.adapter.EventGridRecyclerAdapter
import com.example.eventpal_organization.databinding.ActivityManagerDashBoardBinding
import com.example.eventpal_organization.model.Event
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ManagerDashboard : AppCompatActivity() {
    private lateinit var binding: ActivityManagerDashBoardBinding
    private lateinit var eventRecycler: RecyclerView
    private lateinit var adapter: EventGridRecyclerAdapter
    private val firestoredb = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerDashBoardBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventRecycler = binding.eventsRecycler
        adapter = EventGridRecyclerAdapter(emptyList()) { eventId ->
            val intent = Intent(this, EventInfo::class.java)
            intent.putExtra("eventID", eventId)  // Pass eventID to the next activity
            startActivity(intent)
        }

        eventRecycler.layoutManager = GridLayoutManager(this, 2)
        eventRecycler.adapter = adapter

        fetchEvents()
        fetchUserName(orgID)
        listenForNewNotifications()

        // Add event button click
        binding.addEventButton.setOnClickListener {
            startActivity(Intent(this, ManagerAddEvent::class.java))
        }

        binding.hamburger.setOnClickListener { view ->
            showMenu(view)
        }

        binding.todoCard.setOnClickListener{
            startActivity(Intent(this,ToDo::class.java))
        }

        binding.notifImg.setOnClickListener{
            startActivity(Intent(this,CompletedEvents::class.java))
        }

        binding.recceImg.setOnClickListener{
            startActivity(Intent(this,M_recce::class.java))
        }

        binding.todoImg.setOnClickListener{
            startActivity(Intent(this,ToDo::class.java))
        }
    }


    override fun onResume() {
        super.onResume()
        fetchEvents()
    }

    private fun fetchUserName(orgID : String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val userID = currentUser.uid


        // Assuming user details are stored in a "Users" collection inside the organization
        firestoredb.collection(orgID)
            .document("employees")
            .collection("manager")
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

    private fun showMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.manager_dash_menu, popupMenu.menu) // Updated reference
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    logout()
                    true
                }
                R.id.menu_profile -> {
                    startActivity(Intent(this,UserProfile::class.java))
                    true
                }

                R.id.menu_notifs -> {
                    startActivity(Intent(this,Notifications::class.java))
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }


    private fun fetchEvents() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val managerID = currentUser.uid  // Get the current user's UID

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .whereEqualTo("managerID", managerID) // Fetch only events managed by this user
            .get()
            .addOnSuccessListener { documents ->
                val eventList = mutableListOf<Event>()

                for (document in documents) {
                    val eventId = document.id
                    val eventName = document.getString("Event name") ?: "No Name"
                    val eventDate = document.getString("Date") ?: "No Date"

                    val event = Event(eventId, eventName, eventDate)
                    eventList.add(event)
                }

                adapter.updateEvents(eventList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load events: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logout() {
        // Sign out
        FirebaseAuth.getInstance().signOut()

        // Clear shared preferences
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

    private fun listenForNewNotifications() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val lastNotificationId = sharedPreferences.getString("lastNotificationId", "")

        val orgID = getSharedPreferences("organizationID", MODE_PRIVATE)
            .getString("orgID", "").orEmpty()

        val userRole = sharedPreferences.getString("userRole", "").orEmpty()

        val notificationsRef = FirebaseFirestore.getInstance()
            .collection(orgID)
            .document("employees")
            .collection(userRole)
            .document(user.uid)
            .collection("Notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)

        notificationsRef.addSnapshotListener { snapshots, error ->
            if (error != null || snapshots == null || snapshots.isEmpty) return@addSnapshotListener

            val latestNotification = snapshots.documents.first()
            val notificationId = latestNotification.id


            if (notificationId != lastNotificationId) {
                val title = latestNotification.getString("title") ?: "New Notification"
                val message = latestNotification.getString("message") ?: "You have a new message."

                showNotificationDialog(title,message)

                // Save the new notification id to shared preferences
                sharedPreferences.edit()
                    .putString("lastNotificationId", notificationId)
                    .apply()
            }
        }
    }


    private fun showNotificationDialog( title:String, message: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notification)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        val titleTextView = dialog.findViewById<TextView>(R.id.dialogTitle)
        val messageTextView = dialog.findViewById<TextView>(R.id.dialogMessage)
        val okButton = dialog.findViewById<Button>(R.id.dialogOkButton)

        titleTextView.text = title +" 🤩"
        messageTextView.text = message

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


}
