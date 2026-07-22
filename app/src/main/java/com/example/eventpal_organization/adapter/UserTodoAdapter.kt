package com.example.eventpal_organization

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.model.Task
import com.google.firebase.firestore.FirebaseFirestore

class UserTodoAdapter(
    var tasks: List<Task>,
    private val firestore: FirebaseFirestore,
    private val orgID: String,
    private val userRole: String,
    private val userUID: String
) : RecyclerView.Adapter<UserTodoAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val statusButton: ImageButton = view.findViewById(R.id.statusButton)
        val taskText: TextView = view.findViewById(R.id.taskText)
        val editTask: ImageView = view.findViewById(R.id.editTask)
        val deleteTask: ImageView = view.findViewById(R.id.deleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.checklist_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.taskText.text = task.description
        holder.statusButton.setImageResource(if (task.isCompleted) R.drawable.ic_checked else R.drawable.ic_unchecked)

        // Toggle Task Completion
        holder.statusButton.setOnClickListener {
            val newStatus = !task.isCompleted
            firestore.collection(orgID)
                .document("employees")
                .collection(userRole)
                .document(userUID)
                .collection("TodoList")
                .document(task.id)
                .update("isCompleted", newStatus)
                .addOnSuccessListener {
                    task.isCompleted = newStatus
                    notifyItemChanged(position)
                }
        }

        // Delete Task
        holder.deleteTask.setOnClickListener {
            firestore.collection(orgID)
                .document("employees")
                .collection(userRole)
                .document(userUID)
                .collection("TodoList")
                .document(task.id)
                .delete()
                .addOnSuccessListener {
                    updateList(tasks.filter { it.id != task.id })
                }
        }

        // Edit Task
        holder.editTask.setOnClickListener {
            val editText = EditText(holder.itemView.context)
            editText.setText(task.description)

            val dialog = android.app.AlertDialog.Builder(holder.itemView.context)
                .setTitle("Edit Task")
                .setView(editText)
                .setPositiveButton("Save") { _, _ ->
                    val newDescription = editText.text.toString()
                    firestore.collection(orgID)
                        .document("employees")
                        .collection(userRole)
                        .document(userUID)
                        .collection("TodoList")
                        .document(task.id)
                        .update("description", newDescription)
                        .addOnSuccessListener {
                            task.description = newDescription
                            notifyItemChanged(position)
                        }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.show()
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun updateList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}
