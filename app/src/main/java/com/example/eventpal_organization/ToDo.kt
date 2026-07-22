package com.example.eventpal_organization

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.databinding.ActivityToDoBinding
import com.example.eventpal_organization.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ToDo : AppCompatActivity() {
    private lateinit var binding: ActivityToDoBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var userRole: String
    private lateinit var userUID: String
    private lateinit var orgID: String
    private lateinit var todoAdapter: UserTodoAdapter
    private val tasksList = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityToDoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        userUID = auth.currentUser?.uid ?: return
        userRole = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).getString("userRole", "") ?: ""
        orgID = getSharedPreferences("organizationID", Context.MODE_PRIVATE).getString("orgID", "") ?: ""

        // Initialize RecyclerView
        todoAdapter = UserTodoAdapter(tasksList, firestore, orgID, userRole, userUID)
        binding.checkListRecycler.layoutManager = LinearLayoutManager(this)
        binding.checkListRecycler.adapter = todoAdapter

        // Load tasks
        loadTasks()

        // Add Task Button Click Listener
        binding.addTaskButton.setOnClickListener {
            addNewTask()
        }
    }

    private fun loadTasks() {
        val todoRef = firestore.collection(orgID)
            .document("employees")
            .collection(userRole)
            .document(userUID)
            .collection("TodoList")

        todoRef.get()
            .addOnSuccessListener { documents ->
                tasksList.clear()
                for (document in documents) {
                    val task = Task(
                        id = document.id,
                        description = document.getString("description") ?: "",
                        isCompleted = document.getBoolean("isCompleted") ?: false
                    )
                    tasksList.add(task)
                }
                todoAdapter.updateList(tasksList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addNewTask() {
        val taskInput = EditText(this)
        taskInput.hint = "Enter task description"

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("New Task")
            .setView(taskInput)
            .setPositiveButton("Add") { _, _ ->
                val description = taskInput.text.toString().trim()
                if (!TextUtils.isEmpty(description)) {
                    val newTask = hashMapOf(
                        "description" to description,
                        "isCompleted" to false
                    )

                    val taskRef = firestore.collection(orgID)
                        .document("employees")
                        .collection(userRole)
                        .document(userUID)
                        .collection("TodoList")

                    taskRef.add(newTask)
                        .addOnSuccessListener { documentReference ->
                            val task = Task(id = documentReference.id, description = description, isCompleted = false)
                            todoAdapter.updateList(todoAdapter.tasks + task)
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Task description cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
}
