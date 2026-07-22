package com.example.eventpal_organization.adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.Task
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ChecklistAdapter(
    val tasks: MutableList<Task>,
    private val orgID: String,
    private val eventID: String,
    private val onEdit: (Task) -> Unit,
    private val onDelete: (Task) -> Unit
) : RecyclerView.Adapter<ChecklistAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val taskDescription: TextView = itemView.findViewById(R.id.taskText)
        val taskStatusButton: ImageButton = itemView.findViewById(R.id.statusButton)
        val editBtn: ImageView = itemView.findViewById(R.id.editTask)
        val deleteBtn: ImageView = itemView.findViewById(R.id.deleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checklist_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        // Ensure description is correctly assigned
        holder.taskDescription.text = task.description

        updateUI(holder, task)

        holder.taskStatusButton.setOnClickListener {
            task.isCompleted = !task.isCompleted
            updateTaskStatusInFirestore(task, holder.itemView)
            updateUI(holder, task)
        }

        holder.editBtn.setOnClickListener { onEdit(task) }
        holder.deleteBtn.setOnClickListener { onDelete(task) }
    }

    override fun getItemCount(): Int = tasks.size

    private fun updateTaskStatusInFirestore(task: Task, view: View) {
        val db = Firebase.firestore

        val taskRef = db.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventID)
            .collection("Checklist")
            .document(task.id)

        taskRef.update("isCompleted", task.isCompleted)
            .addOnSuccessListener {
                Log.d("Firestore", "Task ${task.id} updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating task: ${e.message}")
                Toast.makeText(view.context, "Failed to update task", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUI(holder: TaskViewHolder, task: Task) {
        holder.taskStatusButton.setImageResource(
            if (task.isCompleted) R.drawable.ic_checked else R.drawable.ic_unchecked
        )
    }

    fun updateList(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    fun removeTask(task: Task) {
        tasks.remove(task)
        notifyDataSetChanged()
    }
}
