package com.example.eventpal_organization.supervisor

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
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
import com.example.eventpal_organization.Notifications
import com.example.eventpal_organization.OrganizationInput
import com.example.eventpal_organization.Progress
import com.example.eventpal_organization.R
import com.example.eventpal_organization.ToDo
import com.example.eventpal_organization.UserProfile
import com.example.eventpal_organization.adapter.EventGridRecyclerAdapter
import com.example.eventpal_organization.databinding.ActivitySupervisorDashboardBinding
import com.example.eventpal_organization.model.Event
import com.example.eventpal_organization.model.NotificationModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class supervisorDashboard : AppCompatActivity() {
    private lateinit var binding : ActivitySupervisorDashboardBinding
    private lateinit var eventRecycler: RecyclerView
    private lateinit var adapter: EventGridRecyclerAdapter
    private val firestoredb = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupervisorDashboardBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        eventRecycler = binding.eventsRecycler
        adapter = EventGridRecyclerAdapter(emptyList()) { eventId ->
            val intent = Intent(this, SupervisorEventInfo::class.java)
            intent.putExtra("eventID", eventId)  // Pass eventID to the next activity
            startActivity(intent)
        }

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventRecycler.layoutManager = GridLayoutManager(this, 2)
        eventRecycler.adapter = adapter

        fetchEvents()
        fetchUserName(orgID)
        listenForNewNotifications()

        binding.notificationImg.setOnClickListener{
            startActivity(Intent(this,Notifications::class.java))
        }

        binding.hamburger.setOnClickListener { view ->
            showMenu(view)
        }

        binding.superTodo.setOnClickListener{
            startActivity(Intent(this,ToDo::class.java))
        }

    }

    private fun fetchEvents() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val supervisorUID = currentUser.uid  // Get current user's UID

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .get()
            .addOnSuccessListener { eventDocuments ->
                val eventList = mutableListOf<Event>()

                val tasks = mutableListOf<com.google.android.gms.tasks.Task<*>>() // Store async tasks

                for (eventDoc in eventDocuments) {
                    val eventId = eventDoc.id
                    val eventName = eventDoc.getString("Event name") ?: "No Name"
                    val eventDate = eventDoc.getString("Date") ?: "No Date"

                    // Fetch the infoList collection inside each event
                    val task = firestoredb.collection(orgID)
                        .document("Events")
                        .collection("ManagerAssignedEvents")
                        .document(eventId)
                        .collection("infoList")
                        .document("Supervisors")
                        .get()
                        .addOnSuccessListener { supervisorDoc ->
                            for ((supervisorName, supervisorData) in supervisorDoc.data ?: emptyMap()) {
                                if (supervisorData is Map<*, *> && supervisorData["UID"] == supervisorUID) {
                                    // Supervisor's UID matches, add event to list
                                    eventList.add(Event(eventId, eventName, eventDate))
                                    break  // Stop checking once we find a match
                                }
                            }
                        }

                    tasks.add(task)
                }

                // Wait for all tasks to complete before updating the UI
                com.google.android.gms.tasks.Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener {
                        adapter.updateEvents(eventList)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Failed to load events: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch events: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
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
            .collection("supervisor")
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

                // Save the new notification ID to SharedPreferences
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

        titleTextView.text = title +"🤩"
        messageTextView.text = message

        okButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

}