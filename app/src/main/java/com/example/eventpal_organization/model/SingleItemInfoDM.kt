package com.example.eventpal_organization.model

data class SingleItemInfoDM(
    var title: String = "",
    var fields: MutableMap<String, String> = mutableMapOf(), // Correct type for fields
    var id: String = "" // Firestore document ID (if needed)
)


