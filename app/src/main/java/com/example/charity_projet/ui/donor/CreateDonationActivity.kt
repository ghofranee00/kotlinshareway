package com.example.charity_projet.ui.donor

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.charity_projet.R
import com.example.charity_projet.api.RetrofitClient
import com.example.charity_projet.api.SessionManager
import com.example.charity_projet.models.DonationRequest
import com.example.charity_projet.models.Post
import com.example.charity_projet.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateDonationActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var post: Post
    private lateinit var currentUser: User

    private lateinit var tvPostTitle: TextView
    private lateinit var tvPostContent: TextView
    private lateinit var tvPostType: TextView
    private lateinit var spinnerCategory: Spinner
    private lateinit var etRegion: EditText
    private lateinit var etDetails: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_donation)

        sessionManager = SessionManager(this)

        // Get post data from intent - CORRECTION: Utilisez une seule méthode
        val postId = intent.getStringExtra("POST_ID")
        val postContent = intent.getStringExtra("POST_CONTENT")
        val postType = intent.getStringExtra("POST_TYPE")

        // Create a simple post object with basic data
        post = Post(
            idServer = postId,
            contenu = postContent,
            typeDemande = postType
        )

        // Get current user from session
        currentUser = User(
            firstName = sessionManager.getUserName(),
            email = sessionManager.getUserEmail(),
            role = sessionManager.getUserRole()
        )

        initializeViews()
        setupViews() // CORRECTION: Appeler setupViews() sans paramètre
        setupSpinner()
    }

    private fun initializeViews() {
        tvPostTitle = findViewById(R.id.tv_post_title)
        tvPostContent = findViewById(R.id.tv_post_content)
        tvPostType = findViewById(R.id.tv_post_type)
        spinnerCategory = findViewById(R.id.spinner_category)
        etRegion = findViewById(R.id.et_region)
        etDetails = findViewById(R.id.et_details)
        btnSubmit = findViewById(R.id.btn_submit)
        btnCancel = findViewById(R.id.btn_cancel)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupViews() { // CORRECTION: Une seule méthode setupViews()
        // Display post information
        tvPostTitle.text = "Help Request: ${post.typeDemande}"
        tvPostContent.text = post.contenu
        tvPostType.text = "Type: ${post.typeDemande ?: "General"}"

        // Button listeners
        btnSubmit.setOnClickListener {
            createDonation()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupSpinner() {
        val categories = arrayOf(
            "FOOD", "HEALTH", "EDUCATION", "CLOTHING", "HOUSING", "MONEY", "OTHER"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        // Set default category based on post type
        val postType = post.typeDemande?.uppercase()
        when (postType) {
            "NOURRITURE" -> spinnerCategory.setSelection(categories.indexOf("FOOD"))
            "SANTE" -> spinnerCategory.setSelection(categories.indexOf("HEALTH"))
            "EDUCATION" -> spinnerCategory.setSelection(categories.indexOf("EDUCATION"))
            "VETEMENT" -> spinnerCategory.setSelection(categories.indexOf("CLOTHING"))
            "LOGEMENT" -> spinnerCategory.setSelection(categories.indexOf("HOUSING"))
            "ARGENT" -> spinnerCategory.setSelection(categories.indexOf("MONEY"))
            else -> spinnerCategory.setSelection(0)
        }
    }

    private fun createDonation() {
        val region = etRegion.text.toString().trim()
        val details = etDetails.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()

        // Validation
        if (region.isEmpty()) {
            etRegion.error = "Please enter your region"
            return
        }

        if (details.isEmpty()) {
            etDetails.error = "Please enter donation details"
            return
        }

        val donationRequest = DonationRequest(
            postId = post.getId() ?: "",
            donorId = currentUser.getId() ?: "",
            categorie = category,
            region = region,
            details = details,
            images = emptyList() // You can add image upload later
        )

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = sessionManager.fetchAuthToken()
                if (token == null) {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        Toast.makeText(this@CreateDonationActivity, "Not authenticated", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val response = RetrofitClient.instance.createDonation("Bearer $token", donationRequest)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        Toast.makeText(this@CreateDonationActivity, "Donation created successfully!", Toast.LENGTH_LONG).show()

                        // Return to posts with success message
                        val resultIntent = Intent()
                        resultIntent.putExtra("DONATION_CREATED", true)
                        setResult(RESULT_OK, resultIntent)
                        finish()
                    } else {
                        val errorMessage = when (response.code()) {
                            403 -> "You can only create donations for your own account"
                            400 -> "Invalid donation data"
                            else -> "Error: ${response.code()}"
                        }
                        Toast.makeText(this@CreateDonationActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@CreateDonationActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !show
        btnCancel.isEnabled = !show
    }
}