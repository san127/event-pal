package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.Event

class BossClientEventsRecyclerAdapter(
    private var clientEvents: List<Event>,
    private val onClientEventClick: (String) -> Unit
) : RecyclerView.Adapter<BossClientEventsRecyclerAdapter.ClientEventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientEventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_recycler_row, parent, false)
        return ClientEventViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClientEventViewHolder, position: Int) {
        val event = clientEvents[position]
        holder.bind(event)
        holder.itemView.setOnClickListener { onClientEventClick(event.eventId) }
    }

    override fun getItemCount(): Int = clientEvents.size

    fun updateClientEvents(newClientEvents: List<Event>) {
        clientEvents = newClientEvents
        notifyDataSetChanged()
    }

    class ClientEventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventName: TextView = itemView.findViewById(R.id.event_name)
        private val eventDate: TextView = itemView.findViewById(R.id.event_date)

        fun bind(event: Event) {
            eventName.text = event.eventName
            eventDate.text = event.eventDate
        }
    }
}
