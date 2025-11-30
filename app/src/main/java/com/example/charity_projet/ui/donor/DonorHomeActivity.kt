package com.example.charity_projet.ui.donor

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.charity_projet.R
import com.example.charity_projet.api.RetrofitClient
import com.example.charity_projet.api.SessionManager
import com.example.charity_projet.models.Post
import com.example.charity_projet.ui.AboutUsActivity
import com.example.charity_projet.ui.LoginActivity
import com.example.charity_projet.ui.NotificationsActivity
import com.example.charity_projet.ui.ProfileActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DonorHomeActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var btnLogout: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_home)

        sessionManager = SessionManager(this)

        // Vérification d'authentification
        checkAuthentication()

        initializeViews()
        setupAppBarListeners()
        setupNavigation()

        // Afficher un message de bienvenue
        Toast.makeText(this, "Bienvenue Donateur!", Toast.LENGTH_SHORT).show()
    }

    private fun checkAuthentication() {
        val token = sessionManager.fetchAuthToken()
        Log.d("DONOR_AUTH", "Token: ${token?.take(20)}...")

        if (token == null) {
            Log.e("DONOR_AUTH", "No token found")
            redirectToLogin()
            return
        }
    }

    private fun initializeViews() {
        btnNotifications = findViewById(R.id.btn_notifications)
        btnProfile = findViewById(R.id.btn_profile)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun setupAppBarListeners() {
        btnNotifications.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        btnProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun setupNavigation() {
        findViewById<Button>(R.id.nav_home).setOnClickListener {
            // Already on home
            Toast.makeText(this, "Vous êtes sur la page d'accueil", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.nav_about).setOnClickListener {
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Déconnexion")
            .setMessage("Êtes-vous sûr de vouloir vous déconnecter ?")
            .setPositiveButton("Oui") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun performLogout() {
        sessionManager.clearAuth()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}