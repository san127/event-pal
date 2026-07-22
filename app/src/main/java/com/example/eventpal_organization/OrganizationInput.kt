package com.example.eventpal_organization


import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.accountant.accountantDashboard
import com.example.eventpal_organization.boss.bossDashboard
import com.example.eventpal_organization.databinding.ActivityOrganizationInputBinding
import com.example.eventpal_organization.supervisor.supervisorDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class OrganizationInput : AppCompatActivity() {
    private lateinit var binding: ActivityOrganizationInputBinding
    private lateinit var auth: FirebaseAuth
    private val firestore = Firebase.firestore
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrganizationInputBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        auth = Firebase.auth

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


            binding.orgFindButton.setOnClickListener {
                val orgID = binding.orgInputText.text.toString().trim().uppercase()

                if (validateInput(orgID)) {
                    findOrg(orgID)
                }
            }
    }

    private fun validateInput(orgID: String): Boolean {
        return when {
            orgID.isEmpty() -> {
                // Toast.makeText(this, "Organization code cannot be empty", Toast.LENGTH_SHORT).show()
                binding.orgInputText.error = "Please enter a value"
                false
            }
            else -> true
        }
    }

    private fun checkUserRoleAndRedirect(orgID : String) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val roles = listOf("manager", "supervisor", "accountant", "boss")

            for (role in roles) {
                firestore.collection(orgID)
                    .document("employees")
                    .collection(role)
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            when (role) {
                                "manager" -> openDashboard(ManagerDashboard::class.java)
                                "supervisor" -> openDashboard(supervisorDashboard::class.java)
                                "accountant" -> openDashboard(accountantDashboard::class.java)
                                "boss" -> openDashboard(bossDashboard::class.java)
                            }
                            finish()
                            return@addOnSuccessListener

                        }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun openDashboard(dashboardClass: Class<*>) {
        Toast.makeText(this, "Redirecting to your dashboard...", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, dashboardClass))
        finish()  // Close the current activity
    }

    private fun findOrg(orgID: String) {
        firestore.collection(orgID)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    Toast.makeText(this, "Organization exists!", Toast.LENGTH_SHORT).show()

                    //store org id in shared preferences
                    sharedPreferences = getSharedPreferences("organizationID", Context.MODE_PRIVATE)
                    val sharedEdit = sharedPreferences.edit()
                    sharedEdit.putString("orgID", orgID)
                    sharedEdit.apply()

                    checkUserRoleAndRedirect(orgID)

                    startActivity(Intent(this, Signup::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Check your organization code again or Check if your organization is registered", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
