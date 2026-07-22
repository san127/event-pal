package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R

class SupervisorAdapter(
    private var supervisorList: MutableList<Pair<String, String>>, // Pair of (Supervisor Name, Phone Number)
    private val onDeleteClick: (String) -> Unit
) : RecyclerView.Adapter<SupervisorAdapter.SupervisorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupervisorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.supervisor_recyc_item, parent, false)
        return SupervisorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SupervisorViewHolder, position: Int) {
        val (supervisorName, supervisorPhone) = supervisorList[position]
        holder.bind(supervisorName, supervisorPhone)
        holder.btnDelete.setOnClickListener {
            onDeleteClick(supervisorName) // Delete by supervisor name
        }
    }

    override fun getItemCount() = supervisorList.size

    fun updateList(newList: List<Pair<String, String>>) {
        supervisorList.clear()
        supervisorList.addAll(newList)
        notifyDataSetChanged()
    }

    class SupervisorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSupervisorName: TextView = itemView.findViewById(R.id.tvSupervisorName)
        val tvSupervisorPhone: TextView = itemView.findViewById(R.id.tvSupervisorPhone)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(name: String, phone: String) {
            tvSupervisorName.text = name
            tvSupervisorPhone.text = phone
        }
    }
}
