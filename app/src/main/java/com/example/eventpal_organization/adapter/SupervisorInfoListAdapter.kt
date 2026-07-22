package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.EventInfoMainItemModel

class SupervisorInfoListAdapter(
    private val infoList: List<EventInfoMainItemModel>,
    private val onItemClick: (String) -> Unit  // Function to handle item clicks
) : RecyclerView.Adapter<SupervisorInfoListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val listItemName: TextView = itemView.findViewById(R.id.listItemName)

        fun bind(item: EventInfoMainItemModel) {
            listItemName.text = item.id
            itemView.setOnClickListener {
                onItemClick(item.id)  // Handle click event
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.read_only_info_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(infoList[position])
    }

    override fun getItemCount(): Int = infoList.size
}
