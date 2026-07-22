
package com.example.eventpal_organization

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
import com.example.eventpal_organization.adapter.ChecklistAdapter
import com.example.eventpal_organization.databinding.ActivityEventInfoBinding
import com.example.eventpal_organization.model.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class EventInfo : AppCompatActivity() {
    private lateinit var binding : ActivityEventInfoBinding
    private var eventID: String = ""
    private val firestoredb = Firebase.firestore
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var eventName : String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventInfoBinding.inflate(layoutInflater)
        enableEdgeToEdge()
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

        binding.managerInfobtn.setOnClickListener {
            val intent = Intent(this, eventInfoMain::class.java)
            intent.putExtra("eventID", eventID)
            intent.putExtra("eventName", eventName)
            startActivity(intent)
        }

        binding.deleteEvntBtn.setOnClickListener{
            showConfirmationDialog()
        }

        binding.addSuperBtn.setOnClickListener{
            val intent = Intent(this, EventSupervisors::class.java)
            intent.putExtra("eventID", eventID)
            intent.putExtra("documentId", "Supervisors")
            intent.putExtra("eventName", eventName)
            startActivity(intent)
        }

        binding.progressBtn.setOnClickListener{
            val intent = Intent(this, Progress::class.java)
            intent.putExtra("eventID", eventID)
            intent.putExtra("eventName", eventName)
            startActivity(intent)
        }

        binding.editEvntBtn.setOnClickListener{
             showEditEventDialog()
        }

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        binding.completeEvntBtn.setOnClickListener{
            completeEvent(orgID)
        }
    }


    private fun completeEvent(orgID: String) {
        eventID = intent.getStringExtra("eventID").toString()

        val assignedRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)

        val completedRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("CompletedEvents")
            .document(eventID)

        assignedRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val eventData = document.data!!

                // Copy main event document
                completedRef.set(eventData).addOnSuccessListener {
                    // List of known subcollections
                    val subCollections = listOf("Progress", "infoList")
                    var copied = 0

                    for (sub in subCollections) {
                        assignedRef.collection(sub).get().addOnSuccessListener { docs ->
                            val batch = firestoredb.batch()

                            for (doc in docs) {
                                val targetDocRef = completedRef.collection(sub).document(doc.id)
                                batch.set(targetDocRef, doc.data ?: emptyMap<String, Any>())
                            }

                            batch.commit().addOnSuccessListener {
                                copied++
                                if (copied == subCollections.size) {
                                    deleteOriginalEvent(assignedRef)
                                }
                            }.addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to copy $sub: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to read $sub: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    if (subCollections.isEmpty()) {
                        deleteOriginalEvent(assignedRef)
                    }

                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to move event: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error fetching event: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun deleteOriginalEvent(assignedRef: DocumentReference) {
        assignedRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Event marked as completed!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete original event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }




    private fun showEditEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_event, null)

        val eventNameInput = dialogView.findViewById<EditText>(R.id.eventNameInput)
        val clientNameInput = dialogView.findViewById<EditText>(R.id.clientNameInput)
        val phoneInput = dialogView.findViewById<EditText>(R.id.phoneInput)
        val venueInput = dialogView.findViewById<EditText>(R.id.venueInput)
        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val timeInput = dialogView.findViewById<EditText>(R.id.timeInput)
        val budgetInput = dialogView.findViewById<EditText>(R.id.budgetInput)

        // Pre-fill inputs with current event details
        eventNameInput.setText(eventName)
        clientNameInput.setText(binding.eventDetails.text.split("\n")[0].replace("Client: ", ""))
        phoneInput.setText(binding.eventDetails.text.split("\n")[1].replace("Phone: ", ""))
        venueInput.setText(binding.eventDetails.text.split("\n")[2].replace("Venue: ", ""))
        dateInput.setText(binding.eventDetails.text.split("\n")[3].replace("Date: ", ""))
        timeInput.setText(binding.eventDetails.text.split("\n")[4].replace("Time: ", ""))
        budgetInput.setText(binding.eventDetails.text.split("\n")[5].replace("Budget: ", ""))

        AlertDialog.Builder(this)
            .setTitle("Edit Event Details")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .show().also { dialog ->
                val positiveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                positiveButton.setOnClickListener {
                    val eventName = eventNameInput.text.toString().trim()
                    val clientName = clientNameInput.text.toString().trim()
                    val phone = phoneInput.text.toString().trim()
                    val venue = venueInput.text.toString().trim()
                    val date = dateInput.text.toString().trim()
                    val time = timeInput.text.toString().trim()
                    val budget = budgetInput.text.toString().trim()

                    when {
                        eventName.isEmpty() || clientName.isEmpty() || phone.isEmpty() ||
                                venue.isEmpty() || date.isEmpty() || time.isEmpty() || budget.isEmpty() -> {
                            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }

                        !phone.matches(Regex("^\\d{10}$")) -> {
                            Toast.makeText(this, "Phone must be a 10-digit number", Toast.LENGTH_SHORT).show()
                        }

                        budget.toDoubleOrNull() == null || budget.toDouble() < 0 -> {
                            Toast.makeText(this, "Budget must be a positive number", Toast.LENGTH_SHORT).show()
                        }

                        else -> {
                            val updatedEvent = mapOf(
                                "Event name" to eventName,
                                "Client name" to clientName,
                                "Phone" to phone,
                                "Venue" to venue,
                                "Date" to date,
                                "Time" to time,
                                "Budget" to budget
                            )
                            updateEventDetails(updatedEvent)
                            dialog.dismiss()
                        }
                    }
                }
            }
    }


    private fun updateEventDetails(updatedEvent: Map<String, String>) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .update(updatedEvent)
            .addOnSuccessListener {
                Toast.makeText(this, "Event details updated successfully!", Toast.LENGTH_SHORT).show()
                fetchEventDetails() // Refresh UI with updated data
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update event: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    val budget = document.getString("Budget").orEmpty()

                    binding.eventInfoTitle.text = eventName

                    val eventDetails = """
                    Client: $clientName
                    Phone: $phone
                    Venue: $venue
                    Date: $date
                    Time: $time
                    Budget: $budget
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



    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete this event? This action cannot be undone.")
            .setPositiveButton("Yes, Delete") { _, _ ->
                deleteEvent()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        eventID = intent.getStringExtra("eventID").toString()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Event deleted successfully!", Toast.LENGTH_SHORT).show()
                finish() // Close the activity after successful deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete event: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun setupChecklistRecyclerView(orgID : String, eventID : String) {

        checklistAdapter = ChecklistAdapter(mutableListOf(), orgID, eventID,
            onEdit = { task -> showEditTaskDialog(task) },
            onDelete = { task -> showDeleteConfirmationDialog(task) }
        )

        binding.checkListRecycler.apply {
            layoutManager = LinearLayoutManager(this@EventInfo)
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