package com.example.eventpal_organization

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.eventpal_organization.accountant.accountantDashboard
import com.example.eventpal_organization.boss.bossDashboard
import com.example.eventpal_organization.supervisor.supervisorDashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private val firestoredb = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

//        checkUserStatus()

        // Find logo ImageView
        val logo = findViewById<ImageView>(R.id.logo)

        // Load zoom-in animation
        val zoomIn = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        logo.startAnimation(zoomIn)

        checkUserStatus()

        // Delay for 2 seconds, then launch MainActivity
        Handler(Looper.getMainLooper()).postDelayed({

            finish() // Close SplashActivity
        }, 2000) // 2000ms = 2 seconds

}

    private fun checkUserStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val sharedPreferences = getSharedPreferences("organizationID", MODE_PRIVATE)
            val orgID = sharedPreferences.getString("orgID", "").orEmpty()

            if (orgID.isNotEmpty()) {
                findUserRole(orgID, currentUser.uid) { role ->
                    if (role != null) {
                        saveUserRoleToPreferences(role)
                        navigateToRoleDashboard(role)
                    } else {
                        Toast.makeText(this, "Role not found. Please contact your admin.", Toast.LENGTH_SHORT).show()
                        navigateTo(OrganizationInput::class.java)
                    }
                }
            } else {
                Toast.makeText(this, "Organization ID not found. Please log in again.", Toast.LENGTH_SHORT).show()
                navigateTo(OrganizationInput::class.java)
            }
        } else {
            navigateTo(OrganizationInput::class.java) // User is not signed in
        }
    }

    private fun findUserRole(orgID: String, userUID: String, callback: (String?) -> Unit) {
        val roles = listOf("manager", "boss", "supervisor", "accountant")
        var roleFound: String? = null

        for (role in roles) {
            firestoredb.collection(orgID)
                .document("employees")
                .collection(role)
                .document(userUID)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && roleFound == null) {
                        roleFound = role
                        callback(role)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error checking role: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Fallback in case no role is found
        Handler(Looper.getMainLooper()).postDelayed({
            if (roleFound == null) callback(null)
        }, 2000)
    }

    private fun saveUserRoleToPreferences(role: String) {
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userPrefs.edit().apply {
            putString("userRole", role)
            putBoolean("isLoggedIn", true)
            apply()
        }
    }

    private fun navigateToRoleDashboard(role: String) {
        when (role) {
            "boss" -> navigateTo(bossDashboard::class.java)
            "manager" -> navigateTo(ManagerDashboard::class.java)
            "supervisor" -> navigateTo(supervisorDashboard::class.java)
            "accountant" -> navigateTo(accountantDashboard::class.java)
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finish()
    }

}
