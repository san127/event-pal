package com.example.eventpal_organization

import android.content.Context
import android.content.Intent
import android.util.Log

import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.accountant.accountantDashboard
import com.example.eventpal_organization.boss.bossDashboard
import com.example.eventpal_organization.databinding.ActivityLoginBinding
import com.example.eventpal_organization.supervisor.supervisorDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Login : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val firestoredb = FirebaseFirestore.getInstance()
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        binding.orgFindButton.setOnClickListener {
            val email = binding.orgInputText.text.toString().trim()
            val password = binding.manager1Password.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                binding.orgInputText.error = "Enter Email"
                binding.manager1Password.error = "Enter password"
            } else {
                loginUser(email, password)
            }
        }

        binding.signUpText.setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
            finish()
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }


        binding.passwordEyeBtn.setOnClickListener {
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Show Password
                binding.manager1Password.transformationMethod = null
                binding.passwordEyeBtn.setImageResource(R.drawable.ic_eye_closed) // Change icon
            } else {
                // Hide Password
                binding.manager1Password.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                binding.passwordEyeBtn.setImageResource(R.drawable.ic_eye_open) // Change icon
            }

            // Keep cursor at the end of the text
            binding.manager1Password.setSelection(binding.manager1Password.text.length)
        }

    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.manager1Password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.manager1Password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0)
        } else {
            // Show password
            binding.manager1Password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.manager1Password.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_open, 0)
        }
        binding.manager1Password.setSelection(binding.manager1Password.text?.length ?: 0) // Keep cursor at the end
        isPasswordVisible = !isPasswordVisible
    }


    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userUID = auth.currentUser?.uid ?: return@addOnSuccessListener
                getUserRoleAndNavigate(userUID)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Login Failed: invalid credentials", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getUserRoleAndNavigate(userUID: String) {
        val sharedPreferences = getSharedPreferences("organizationID", Context.MODE_PRIVATE)
        val orgID = sharedPreferences.getString("orgID", "").orEmpty()
        val roles = listOf("manager", "boss", "supervisor", "accountant")

        if (orgID.isEmpty()) {
            Toast.makeText(this, "Organization ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Iterate through roles and check if the user exists in any role collection
        roles.forEach { role ->
            firestoredb.collection(orgID)
                .document("employees")
                .collection(role)
                .document(userUID)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        // Role found, save it to SharedPreferences
                        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        userPrefs.edit().apply {
                            putString("userRole", role)
                            putBoolean("isLoggedIn", true)
                            apply()
                        }


                        // Navigate to the corresponding dashboard
                        when (role) {
                            "manager" -> startActivity(Intent(this, ManagerDashboard::class.java))
                            "boss" -> startActivity(Intent(this, bossDashboard::class.java))
                            "supervisor" -> startActivity(Intent(this, supervisorDashboard::class.java))
                            "accountant" -> startActivity(Intent(this, accountantDashboard::class.java))
                        }
                        finish() // End Login activity
                        return@addOnSuccessListener
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking user role: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Reset Password")

        val input = android.widget.EditText(this)
        input.hint = "Enter your email"
        input.inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        builder.setView(input)

        builder.setPositiveButton("Send") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun sendPasswordResetEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
