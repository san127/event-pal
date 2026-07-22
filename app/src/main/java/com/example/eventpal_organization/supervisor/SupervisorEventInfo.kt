package com.example.eventpal_organization.supervisor

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventpal_organization.Progress
import com.example.eventpal_organization.R
import com.example.eventpal_organization.adapter.ChecklistAdapter
import com.example.eventpal_organization.databinding.ActivitySupervisorEventInfoBinding
import com.example.eventpal_organization.model.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SupervisorEventInfo : AppCompatActivity() {
    private lateinit var binding : ActivitySupervisorEventInfoBinding
    private var eventID: String = ""
    private val firestoredb = Firebase.firestore
    private lateinit var checklistAdapter: ChecklistAdapter
    private var eventName : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySupervisorEventInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        eventID = intent.getStringExtra("eventID").toString()

        if (eventID.isNotEmpty()) {
            fetchEventDetails()
        } else {
            Toast.makeText(this, "No Event ID found", Toast.LENGTH_SHORT).show()
        }

        setupChecklistRecyclerView(orgID, eventID)
        fetchChecklist(orgID, eventID)

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        binding.progressBtn.setOnClickListener{
            val intent = Intent(this, Progress::class.java)
            intent.putExtra("eventID", eventID)
            intent.putExtra("eventName", eventName)// Pass eventID to the next activity
            startActivity(intent)
        }

        binding.supervisorInfobtn.setOnClickListener {
            val intent = Intent(this, SupervisorInfoList::class.java)
            intent.putExtra("eventID", eventID)
            intent.putExtra("eventName", eventName)
            startActivity(intent)
        }
    }

    private fun fetchEventDetails() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    eventName = document.getString("Event name").orEmpty()
                    val clientName = document.getString("Client name").orEmpty()
                    val phone = document.getString("Phone").orEmpty()
                    val venue = document.getString("Venue").orEmpty()
                    val date = document.getString("Date").orEmpty()
                    val time = document.getString("Time").orEmpty()
                    //val budget = document.getString("Budget").orEmpty()

                    binding.eventInfoTitle.text = eventName

                    val eventDetails = """
                    Client: $clientName
                    Phone: $phone
                    Venue: $venue
                    Date: $date
                    Time: $time
                """.trimIndent()

                    binding.eventDetails.text = eventDetails
                } else {
                    Toast.makeText(this, "Event details not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch event details: ${e.message}", Toast.LENGTH_SHORT).show()
            }


    }

    private fun setupChecklistRecyclerView(orgID : String, eventID : String) {

        checklistAdapter = ChecklistAdapter(mutableListOf(), orgID, eventID,
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task -> showDeleteConfirmationDialog(task) }
        )

        binding.checkListRecycler.apply {
            layoutManager = LinearLayoutManager(this@SupervisorEventInfo)
            adapter = checklistAdapter
        }
    }

    private fun fetchChecklist(orgID: String, eventID: String) {
        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Checklist")
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.map { doc ->
                    Task(
                        id = doc.id,
                        description = doc.getString("description") ?: "",
                        isCompleted = doc.getBoolean("isCompleted") ?: false
                    )
                }
                checklistAdapter.updateList(tasks)
            }
    }

    private fun showAddTaskDialog() {
        val taskInput = EditText(this)
        AlertDialog.Builder(this)
            .setTitle("Add Task")
            .setView(taskInput)
            .setPositiveButton("Add") { _, _ ->
                val taskDesc = taskInput.text.toString()
                if (taskDesc.isNotEmpty()) {
                    addTaskToFirestore(taskDesc)
                } else {
                    Toast.makeText(this, "Task cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTaskToFirestore(description: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()
        val newTask = hashMapOf(
            "description" to description,
            "isCompleted" to false
        )

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Checklist")
            .add(newTask)
            .addOnSuccessListener { documentReference ->
                val task = Task(id = documentReference.id, description = description, isCompleted = false)
                checklistAdapter.updateList(checklistAdapter.tasks + task)
            }
    }

    private fun showEditTaskDialog(task: Task) {
        val taskInput = EditText(this)
        taskInput.setText(task.description)

        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(taskInput)
            .setPositiveButton("Update") { _, _ ->
                val updatedDesc = taskInput.text.toString()
                if (updatedDesc.isNotEmpty()) {
                    updateTaskInFirestore(task.copy(description = updatedDesc))
                } else {
                    Toast.makeText(this, "Task cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTaskInFirestore(task: Task) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Checklist")
            .document(task.id)
            .update("description", task.description)
            .addOnSuccessListener {
                fetchChecklist(orgID, eventID)
            }
    }

    private fun showDeleteConfirmationDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Checklist")
            .document(task.id)
            .delete()
            .addOnSuccessListener {
                checklistAdapter.removeTask(task)
                Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show()
            }
    }

}