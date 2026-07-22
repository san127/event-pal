package com.example.eventpal_organization.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventpal_organization.R
import com.example.eventpal_organization.model.NotificationModel
import java.text.SimpleDateFormat
import java.util.*

class NotificationAdapter(private val notificationList: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val timestamp: TextView = view.findViewById(R.id.notificationTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notificationList[position]

        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.timestamp.text = formatTimestamp(notification.timestamp)
    }

    override fun getItemCount(): Int = notificationList.size

    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp?): String {
        return if (timestamp != null) {
            val date = timestamp.toDate()
            val format = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
            format.format(date)
        } else {
            "Unknown time"
        }
    }
}
