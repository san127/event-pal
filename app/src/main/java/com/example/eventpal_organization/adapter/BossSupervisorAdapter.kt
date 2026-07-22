package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R

class BossSupervisorAdapter (
    private var supervisorList: MutableList<Pair<String, String>>, // Pair of (Supervisor Name, Phone Number)
) : RecyclerView.Adapter<BossSupervisorAdapter .SupervisorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupervisorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.boss_supervisor_item, parent, false)
        return SupervisorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SupervisorViewHolder, position: Int) {
        val (supervisorName, supervisorPhone) = supervisorList[position]
        holder.bind(supervisorName, supervisorPhone)
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

        fun bind(name: String, phone: String) {
            tvSupervisorName.text = name
            tvSupervisorPhone.text = phone
        }
    }
}
