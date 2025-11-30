package com.example.charity_projet.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.charity_projet.R
import com.example.charity_projet.api.RetrofitClient
import com.example.charity_projet.api.SessionManager
import com.example.charity_projet.models.PasswordUpdateRequest
import com.example.charity_projet.models.UserUpdateRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)

        // Initialiser les vues
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // ⚠️ CORRECTION: Charger les données depuis l'Intent
        loadDataFromIntent()

        btnSave.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProfile(firstName, lastName, email)
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadDataFromIntent() {
        // Récupérer les données depuis l'Intent
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""
        val email = intent.getStringExtra("email") ?: ""

        // Remplir les champs avec les données reçues
        etFirstName.setText(firstName)
        etLastName.setText(lastName)
        etEmail.setText(email)

        // Si les données sont vides, essayer de charger depuis l'API
        if (firstName.isEmpty() && lastName.isEmpty() && email.isEmpty()) {
            loadCurrentProfile()
        } else {
            Toast.makeText(this, "Profile data loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentProfile() {
        val token = sessionManager.fetchAuthToken()
        val username = sessionManager.getUsername()

        if (token != null && username != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getProfile("Bearer $token")

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val user = response.body()
                            user?.let {
                                // Remplir les champs avec les données actuelles
                                etFirstName.setText(it.firstName ?: "")
                                etLastName.setText(it.lastName ?: "")
                                etEmail.setText(it.email ?: "")

                                Toast.makeText(this@EditProfileActivity, "Profile loaded from API", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Fallback: utiliser les données locales
                            loadFromLocalData()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        // Fallback: utiliser les données locales
                        loadFromLocalData()
                    }
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadFromLocalData() {
        // Utiliser les données sauvegardées localement
        val fullName = sessionManager.getUserName().orEmpty()
        val email = sessionManager.getUserEmail().orEmpty()

        // Séparer prénom et nom
        val nameParts = fullName.split(" ")
        if (nameParts.size >= 2) {
            etFirstName.setText(nameParts[0])
            etLastName.setText(nameParts.subList(1, nameParts.size).joinToString(" "))
        } else if (nameParts.size == 1) {
            etFirstName.setText(fullName)
        }
        etEmail.setText(email)

        Toast.makeText(this, "Using local data", Toast.LENGTH_SHORT).show()
    }

    private fun updateProfile(firstName: String, lastName: String, email: String) {
        val username = sessionManager.getUsername()
        if (username != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = UserUpdateRequest(firstName, lastName, email)
                    val response = RetrofitClient.instance.updateProfile(username, request)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            // Mettre à jour les données locales
                            sessionManager.saveUserInfo(
                                username = username,
                                name = "$firstName $lastName",
                                email = email,
                                role = sessionManager.getUserRole() ?: "USER"
                            )

                            Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(this@EditProfileActivity, "Failed to update: ${errorBody ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(dialogView)
            .setPositiveButton("Change") { dialog, which ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                changePassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val username = sessionManager.getUsername()
        if (username != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = PasswordUpdateRequest(currentPassword, newPassword)
                    val response = RetrofitClient.instance.updatePassword(username, request)

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@EditProfileActivity, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Toast.makeText(this@EditProfileActivity, "Failed to change password: ${errorBody ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@EditProfileActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
        }
    }
}