package com.example.eventpal_organization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Signup : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private val firestoredb = Firebase.firestore
    private var userUID = ""
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // Initialize FirebaseAuth
        auth = Firebase.auth

        // Retrieve organization name
        getOrgName()

        binding.loginText.setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        val suggestions = listOf("--select role--", "Boss", "Manager", "Supervisor", "Accountant")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suggestions)
        binding.roleInputSpinner.adapter = adapter

        var selectedValue = "--select role--"

        binding.roleInputSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedValue = parent.getItemAtPosition(position).toString()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle the case when nothing is selected
                Toast.makeText(this@Signup, "Please select a role.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.signUpCreateBtn.setOnClickListener {

            val name = binding.signupName.text.toString().trim()
            val phone = binding.signupPhone.text.toString().trim()
            val email = binding.signUpEmail.text.toString().trim()
            val password = binding.signUpPassword.text.toString().trim()
            val roleCode = binding.roleCodeEditText.text.toString().trim()

            // Validate inputs
            if (name.isEmpty()) {
                binding.signupName.error = "Name is required"
                return@setOnClickListener
            } else if (!name.matches(Regex("^[a-zA-Z\\s]+$"))) {
                binding.signupName.error = "Name must only contain letters"
                return@setOnClickListener
            }

            if (phone.isEmpty() || !phone.matches(Regex("^[0-9]{10}$"))) {
                binding.signupPhone.error = "Enter a valid 10-digit phone number"
                return@setOnClickListener
            }

            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.signUpEmail.error = "Enter a valid email address"
                return@setOnClickListener
            }

            if (password.isEmpty() || password.length < 6) {
                binding.signUpPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            }

            if (roleCode.isEmpty()) {
                binding.roleCodeEditText.error = "Role code is required"
                return@setOnClickListener
            }

            if (selectedValue == "--select role--") {
                Toast.makeText(this, "Please select a valid role.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            } else {
                storeNewUserDetail(selectedValue.lowercase())
            }
        }

        binding.passEyeBtn.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Show Password
                binding.signUpPassword.transformationMethod = null
                binding.passEyeBtn.setImageResource(R.drawable.ic_eye_closed) // Change icon
            } else {
                // Hide Password
                binding.signUpPassword.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding.passEyeBtn.setImageResource(R.drawable.ic_eye_open) // Change icon
            }

            // Keep cursor at the end of the text
            binding.signUpPassword.setSelection(binding.signUpPassword.text.length)
        }


    }




    private fun signUpUser(email: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    userUID = auth.currentUser?.uid.toString()
                    onSuccess()

                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            }
    }

    private fun storeNewUserDetail(userRole: String) {
        val sharedPreferences = getSharedPreferences("organizationID", Context.MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        if (orgID.isEmpty()) {
            Toast.makeText(this, "Organization ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        val roleCode = binding.roleCodeEditText.text.toString().trim()
        if (roleCode.isEmpty()) {
            Toast.makeText(this, "Please enter a role code.", Toast.LENGTH_SHORT).show()
            return
        }

        firestoredb.collection(orgID)
            .document("employees")
            .get()
            .addOnSuccessListener { document ->
                val accessCode = document.getString(userRole)
                if (accessCode == roleCode) {
                    val userData = mapOf(
                        "Name" to binding.signupName.text.toString().trim(),
                        "Phone" to binding.signupPhone.text.toString().trim(),
                        "Role" to userRole
                    )

                    signUpUser(
                        binding.signUpEmail.text.toString().trim(),
                        binding.signUpPassword.text.toString().trim(),
                        onSuccess = {
                            val userRef = firestoredb.collection(orgID)
                                .document("employees")
                                .collection(userRole)
                                .document(userUID)

                            // Save user details
                            userRef.set(userData)
                                .addOnSuccessListener {
                                    // Create an empty Notifications collection
//                                    val notificationRef = userRef.collection("Notifications").document()
//                                    notificationRef.set(emptyMap<String, Any>())

                                    val notificationRef = userRef.collection("Notifications").document()


                                    // Save user role and login status in SharedPreferences
                                    saveUserRoleAndStatus(userRole)
                                    Toast.makeText(this, "Sign Up successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, Login::class.java))
                                    finish()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
                                }
                        },
                        onFailure = {
                            Toast.makeText(this, "Sign up failed.", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(this, "Role code doesn't match.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch role codes.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveUserRoleAndStatus(userRole: String) {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("userRole", userRole)
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }


    private fun getOrgName() {
        val sharedPreferences = getSharedPreferences("organizationID", Context.MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()

        if (orgID.isNotEmpty()) {
            firestoredb.collection(orgID)
                .document("OrgDetails")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        binding.orgidText.text = document.getString("OrgName")
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to fetch organization name", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

