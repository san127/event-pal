package com.example.eventpal_organization.model
import com.google.firebase.Timestamp

data class NotificationModel(
    val title: String = "",
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now()
)
