package com.example.eventpal_organization

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.example.eventpal_organization.databinding.ActivityUserProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class UserProfile : AppCompatActivity() {
    private lateinit var binding : ActivityUserProfileBinding
    private val firestoredb = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        fetchUserProfile()

        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = userPrefs.getString("userRole", "") ?: ""

        binding.userRoleText.text = role.capitalize()

        binding.nameEditBtn.setOnClickListener {
            showEditDialog("Name", binding.nameEditText.text.toString()) { newName ->
                updateFirestore("Name", newName)
            }
        }

        binding.phoneEditBtn.setOnClickListener {
            showEditDialog("Phone", binding.phoneText.text.toString()) { newPhone ->
                updateFirestore("Phone", newPhone)
            }
        }

    }

    private fun fetchUserProfile() {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = userPrefs.getString("userRole", "") ?: ""
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (orgID.isNotEmpty() && role.isNotEmpty() && userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection(orgID).document("employees")
                .collection(role).document(userId)

            userRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("Name") ?: "N/A"
                        val phone = document.getString("Phone") ?: "N/A"

                        binding.nameEditText.setText(name)
                        binding.phoneText.setText(phone)

                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to fetch data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showEditDialog(fieldName: String, currentValue: String, onSave: (String) -> Unit) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Edit $fieldName")

        val input = EditText(this)
        input.setText(currentValue)
        input.inputType = InputType.TYPE_CLASS_TEXT
        dialogBuilder.setView(input)

        dialogBuilder.setPositiveButton("Save") { _, _ ->
            val newValue = input.text.toString()
            onSave(newValue)
        }

        dialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }

        dialogBuilder.create().show()
    }

    private fun updateFirestore(field: String, newValue: String) {
        val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = userPrefs.getString("userRole", "") ?: ""
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (orgID.isNotEmpty() && role.isNotEmpty() && userId != null) {
            val userRef = firestoredb.collection(orgID).document("employees")
                .collection(role).document(userId)

            userRef.update(field, newValue)
                .addOnSuccessListener {
                    Toast.makeText(this, "$field updated successfully!", Toast.LENGTH_SHORT).show()
                    if (field == "Name") {
                        binding.nameEditText.setText(newValue)
                    } else if (field == "Phone") {
                        binding.phoneText.setText(newValue)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }




}