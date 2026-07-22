package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.Event

class BossEventsRecyclerAdapter(
    private var events: List<Event>,
    private val onEventClick: (String) -> Unit
) : RecyclerView.Adapter<BossEventsRecyclerAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_recycler_row, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
        holder.itemView.setOnClickListener {
            onEventClick(event.eventId)  // Pass eventId on click
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventName: TextView = itemView.findViewById(R.id.event_name)
        private val eventDate: TextView = itemView.findViewById(R.id.event_date)

        fun bind(event: Event) {
            eventName.text = event.eventName
            eventDate.text = event.eventDate
        }
    }
}

