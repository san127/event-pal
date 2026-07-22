package com.example.eventpal_organization

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.adapter.NotificationAdapter
import com.example.eventpal_organization.databinding.ActivityNotificationsBinding
import com.example.eventpal_organization.model.NotificationModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Notifications : AppCompatActivity() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var notificationAdapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationModel>()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
private lateinit var binding : ActivityNotificationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = binding.notifRecycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificationAdapter = NotificationAdapter(notificationList)
        recyclerView.adapter = notificationAdapter

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        if (orgID.isEmpty()) {
            Toast.makeText(this, "Organization ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRole = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("userRole", "").orEmpty()

        val userDocRef = firestore.collection(orgID)
            .document("employees")
            .collection(userRole)
            .document(user.uid)
            .collection("Notifications")

        userDocRef.get()
            .addOnSuccessListener { documents ->
                notificationList.clear()
                for (document in documents) {
                    val title = document.getString("title") ?: "No name"
                    val message = document.getString("message") ?: "No date"
                    val timestamp = document.getTimestamp("timestamp") ?: Timestamp.now()

                    val notification = NotificationModel(title, message, timestamp)
                    notificationList.add(notification)
                }
                notificationAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching notifications", e)
                Toast.makeText(this, "Failed to fetch notifications.", Toast.LENGTH_SHORT).show()
            }
    }




}