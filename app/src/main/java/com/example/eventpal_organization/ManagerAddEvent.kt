package com.example.eventpal_organization

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.databinding.ActivityManagerAddEventBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class ManagerAddEvent : AppCompatActivity() {
    private lateinit var binding: ActivityManagerAddEventBinding
    private val calendar = Calendar.getInstance()
    private val firestoredb = Firebase.firestore
    private val storageRef = FirebaseStorage.getInstance().reference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagerAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//       binding.dateInput.showSoftInputOnFocus = false
//       binding.timeInput.showSoftInputOnFocus = false
//
//        binding.dateInput.setOnClickListener { showDatePicker() }
//        binding.timeInput.setOnClickListener { showTimePicker() }

        binding.dateInput.isFocusable = false
        binding.dateInput.isClickable = true
        binding.dateInput.setOnClickListener {
            showDatePicker()
        }

        binding.timeInput.isFocusable = false
        binding.timeInput.isClickable = true
        binding.timeInput.setOnClickListener {
            showTimePicker()
        }

        binding.addButton.setOnClickListener {
            if (validateInputs()) {
                addEventDB()
            }
        }
    }

    private fun showDatePicker() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val formattedDate =
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
                binding.dateInput.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun showTimePicker() {
        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val formattedTime =
                    SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                binding.timeInput.setText(formattedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false // Use 12-hour format (AM/PM)
        )
        timePicker.show()
    }

    private fun validateInputs(): Boolean {
        val phone = binding.phoneInput.text.toString().trim()

        if (binding.eventNameInput.text.toString().trim().isEmpty()) {
            binding.eventNameInput.error = "Event name is required"
            return false
        }
        if (binding.clientNameAutoInput.text.toString().trim().isEmpty()) {
            binding.clientNameAutoInput.error = "Client name is required"
            return false
        }
        if (binding.dateInput.text.toString().trim().isEmpty()) {
            binding.dateInput.error = "Date is required"
            return false
        }
        if (binding.timeInput.text.toString().trim().isEmpty()) {
            binding.timeInput.error = "Time is required"
            return false
        }
        if (binding.venueInput.text.toString().trim().isEmpty()) {
            binding.venueInput.error = "Venue is required"
            return false
        }
        if (binding.budgetInput.text.toString().trim().isEmpty()) {
            binding.budgetInput.error = "Budget is required"
            return false
        }
        if (phone.isEmpty() || !phone.matches("\\d{10}".toRegex())) {
            binding.phoneInput.error = "Enter a valid 10-digit phone number"
            return false
        }

        return true
    }

    private fun addEventDB() {
        val sharedPreferences = getSharedPreferences("organizationID", Context.MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        if (orgID.isEmpty()) {
            Toast.makeText(this, "Organization ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        val managerID = currentUser.uid
        val newEventId = UUID.randomUUID().toString() // Unique Event ID

        val newEventDetails = mapOf(
            "Event name" to binding.eventNameInput.text.toString().trim(),
            "Client name" to binding.clientNameAutoInput.text.toString().trim(),
            "Date" to binding.dateInput.text.toString().trim(),
            "Time" to binding.timeInput.text.toString().trim(),
            "Phone" to binding.phoneInput.text.toString().trim(),
            "Venue" to binding.venueInput.text.toString().trim(),
            "Budget" to binding.budgetInput.text.toString().trim(),
            "managerID" to managerID
        )

        firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(newEventId)
            .set(newEventDetails)
            .addOnSuccessListener {
                createInfoList(orgID, newEventId)
                createStorageFolders(newEventId) // 🔥 Creates Storage Folders
                Toast.makeText(this, "New event added successfully!", Toast.LENGTH_SHORT).show()
                finish()
                startActivity(Intent(this, ManagerDashboard::class.java))
            }
            .addOnFailureListener {
                Toast.makeText(this, "Unable to add event", Toast.LENGTH_SHORT).show()
            }
    }



    private fun createInfoList(orgID: String, eventId: String) {
        val infoList = listOf("Bills")
        val eventDocRef = firestoredb.collection(orgID)
            .document("Events")
            .collection("ManagerAssignedEvents")
            .document(eventId)
            .collection("infoList")

        for (info in infoList) {
            eventDocRef.document(info).set(emptyMap<String, Any>()) // Create an empty document
                .addOnFailureListener { e ->
                    //Toast.makeText(this, "Failed to create $info: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun createStorageFolders(eventId: String) {
        val folders = listOf("bills")

        for (folder in folders) {
            val folderRef = storageRef.child("events/$eventId/$folder/placeholder.txt")
            val placeholderData = "This is a placeholder file to keep the folder".toByteArray()

            folderRef.putBytes(placeholderData)
                .addOnSuccessListener {
                    //Toast.makeText(this, "Created $folder folder in Storage", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to create $folder folder", Toast.LENGTH_SHORT).show()
                }
        }
    }

//    private fun createInfoList(orgID: String, eventId: String) {
//        val infoList = listOf("Bills", "Inspirations")
//        val eventDocRef = firestoredb.collection(orgID)
//            .document("Events")
//            .collection("ManagerAssignedEvents")
//            .document(eventId)
//            .collection("infoList")
//
//        for (info in infoList) {
//            eventDocRef.document(info).set(emptyMap<String, Any>()) // Create an empty document
//                .addOnFailureListener { e ->
//                    //Toast.makeText(this, "Failed to create $info: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }

//    private fun createStorageFolders(eventId: String) {
//        val folders = listOf("bills", "Inspirations")
//
//        for (folder in folders) {
//            val folderRef = storageRef.child("events/$eventId/$folder/placeholder.txt")
//            val placeholderData = "This is a placeholder file to keep the folder".toByteArray()
//
//            folderRef.putBytes(placeholderData)
//                .addOnSuccessListener {
//                    //Toast.makeText(this, "Created $folder folder in Storage", Toast.LENGTH_SHORT).show()
//                }
//                .addOnFailureListener {
//                    Toast.makeText(this, "Failed to create $folder folder", Toast.LENGTH_SHORT).show()
//                }
//        }
//    }
}
