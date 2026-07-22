package com.example.eventpal_organization

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.databinding.ActivityProgressBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Progress : AppCompatActivity() {

    private lateinit var progressContainer: LinearLayout
    private val firestoredb = Firebase.firestore
    private var eventID: String = ""
    private lateinit var binding: ActivityProgressBinding
    private var eventName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }


        eventName = intent.getStringExtra("eventName").toString()
        binding.eventName.text = eventName

        eventID = intent.getStringExtra("eventID").toString()
        progressContainer = binding.progressContainer
        loadProgressBars()


    }

    private fun loadProgressBars() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Progress")
            .get()
            .addOnSuccessListener { documents ->
                progressContainer.removeAllViews()
                for (document in documents) {
                    val taskName = document.id
                    val progressValue = document.getLong("status")?.toInt() ?: 0
                    addProgressBar(taskName, progressValue)
                }
            }
    }




    private fun addProgressBar(taskName: String, progressValue: Int) {
        val inflater = LayoutInflater.from(this)
        val taskView = inflater.inflate(R.layout.single_progress_item, progressContainer, false)

        val taskTitle = taskView.findViewById<TextView>(R.id.taskTitle)
        val progressBar = taskView.findViewById<ProgressBar>(R.id.taskProgressBar)
        val updateButton = taskView.findViewById<Button>(R.id.updateProgressButton)
        val reduceButton = taskView.findViewById<Button>(R.id.reduceProgressButton)
        val editButton = taskView.findViewById<ImageButton>(R.id.editTaskButton)
        val deleteButton = taskView.findViewById<ImageButton>(R.id.deleteTaskButton)

        taskTitle.text = taskName
        progressBar.progress = progressValue

        updateButton.setOnClickListener {
            val newProgress = progressBar.progress + 10
            if (newProgress <= 100) {
                progressBar.progress = newProgress
                updateProgressInFirestore(taskName, newProgress)
            }
        }

        reduceButton.setOnClickListener {
            val newProgress = progressBar.progress - 10
            if (newProgress >= 0) {
                progressBar.progress = newProgress
                updateProgressInFirestore(taskName, newProgress)
            }
        }

        editButton.setOnClickListener {
            showEditTaskDialog(taskName, taskView)
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmationDialog(taskName, taskView)
        }

        val percentageText = taskView.findViewById<TextView>(R.id.progressPercentageText)
        percentageText.text = "$progressValue%"

        updateButton.setOnClickListener {
            val newProgress = progressBar.progress + 10
            if (newProgress <= 100) {
                progressBar.progress = newProgress
                percentageText.text = "$newProgress%"
                updateProgressInFirestore(taskName, newProgress)
            }
        }

        reduceButton.setOnClickListener {
            val newProgress = progressBar.progress - 10
            if (newProgress >= 0) {
                progressBar.progress = newProgress
                percentageText.text = "$newProgress%"
                updateProgressInFirestore(taskName, newProgress)
            }
        }


        progressContainer.addView(taskView)
    }

    private fun updateProgressInFirestore(taskName: String, newProgress: Int) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val progressRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Progress")
            .document(taskName)

        progressRef.set(mapOf("status" to newProgress))
            .addOnSuccessListener {
                // Success Message
            }
            .addOnFailureListener {
                // Failure Message
            }
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editTextTaskName = dialogView.findViewById<EditText>(R.id.editTextTaskName)

        AlertDialog.Builder(this)
            .setTitle("Add New Task")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val taskName = editTextTaskName.text.toString().trim()
                if (taskName.isNotEmpty()) {
                    addTaskToFirestore(taskName)
                } else {
                    Toast.makeText(this, "Task name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTaskToFirestore(taskName: String) {
        val taskData = hashMapOf("status" to 0)
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Progress")
            .document(taskName)
            .set(taskData)
            .addOnSuccessListener {
                addProgressBar(taskName, 0)
                Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditTaskDialog(oldTaskName: String, taskView: View) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val editTextTaskName = dialogView.findViewById<EditText>(R.id.editTextTaskName)
        editTextTaskName.setText(oldTaskName)

        AlertDialog.Builder(this)
            .setTitle("Edit Task Name")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newTaskName = editTextTaskName.text.toString().trim()
                if (newTaskName.isNotEmpty() && newTaskName != oldTaskName) {
                    updateTaskNameInFirestore(oldTaskName, newTaskName, taskView)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTaskNameInFirestore(oldTaskName: String, newTaskName: String, taskView: View) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        val progressCollection = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Progress")

        progressCollection.document(oldTaskName).get()
            .addOnSuccessListener { documentSnapshot ->
                val status = documentSnapshot.getLong("status") ?: 0
                progressCollection.document(oldTaskName).delete()
                progressCollection.document(newTaskName).set(mapOf("status" to status))
                taskView.findViewById<TextView>(R.id.taskTitle).text = newTaskName
            }
    }

    private fun showDeleteConfirmationDialog(taskName: String, taskView: View) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete $taskName?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTaskFromFirestore(taskName, taskView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTaskFromFirestore(taskName: String, taskView: View) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Progress")
            .document(taskName)
            .delete()
            .addOnSuccessListener {
                progressContainer.removeView(taskView)
            }
    }
}
