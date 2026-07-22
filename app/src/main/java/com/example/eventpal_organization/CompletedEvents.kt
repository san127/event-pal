package com.example.eventpal_organization


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.accountant.AccountantBillsInfo
import com.example.eventpal_organization.adapter.BossEventInfoListAdapter
import com.example.eventpal_organization.adapter.BossEventsRecyclerAdapter
import com.example.eventpal_organization.adapter.EventGridRecyclerAdapter
import com.example.eventpal_organization.boss.BossAttachments
import com.example.eventpal_organization.boss.BossSingleEventInfo
import com.example.eventpal_organization.boss.BossSupervisors
import com.example.eventpal_organization.databinding.ActivityAccountantDashboardBinding
import com.example.eventpal_organization.databinding.ActivityBossEventInfoBinding
import com.example.eventpal_organization.databinding.ActivityCompletedEventsBinding
import com.example.eventpal_organization.model.Event
import com.example.eventpal_organization.model.EventInfoMainItemModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore

    import com.google.firebase.ktx.Firebase

    class CompletedEvents : AppCompatActivity() {
        private lateinit var binding : ActivityCompletedEventsBinding
        private lateinit var eventRecycler: RecyclerView
        private lateinit var adapter: EventGridRecyclerAdapter
        private val firestoredb = Firebase.firestore
        private lateinit var bossEventsAdapter: BossEventsRecyclerAdapter

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityCompletedEventsBinding.inflate(layoutInflater)
            enableEdgeToEdge()
            setContentView(binding.root)
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
            val orgID = sharedPreferences.getString("orgID", "").orEmpty()


            fetchEvents(orgID)

            setupRecyclerViews()
        }

        private fun fetchEvents(orgID : String) {
            firestoredb.collection(orgID)
                .document("Events")
                .collection("CompletedEvents").get()
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


        private fun setupRecyclerViews() {
            // Setup RecyclerView for Events assigned by managers
            bossEventsAdapter = BossEventsRecyclerAdapter(emptyList()) { eventID ->
                val intent = Intent(this, CompletedEventInfo::class.java)
                intent.putExtra("eventID", eventID)  // Pass eventId to BossEventInfo
                startActivity(intent)
            }
            binding.eventsRecycler.apply {
                layoutManager = GridLayoutManager(this@CompletedEvents, 2)
                adapter = bossEventsAdapter
            }
        }

    }