package com.example.charity_projet.ui.donor


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.charity_projet.R
import com.example.charity_projet.api.RetrofitClient
import com.example.charity_projet.api.SessionManager
import com.example.charity_projet.models.DonationRequest
import com.example.charity_projet.ui.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateDonationActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerRegion: Spinner
    private lateinit var etDetails: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnAddImage: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewImages: RecyclerView
    private lateinit var tvPostTitle: TextView

    private var selectedImages = mutableListOf<String>()
    private var postId: String? = null
    private var postTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_donation)

        sessionManager = SessionManager(this)
        initializeViews()
        setupClickListeners()
        loadIntentData()
    }

    private fun initializeViews() {
        spinnerCategory = findViewById(R.id.spinner_category)
        spinnerRegion = findViewById(R.id.spinner_region)
        etDetails = findViewById(R.id.et_details)
        btnSubmit = findViewById(R.id.btn_submit)
        btnAddImage = findViewById(R.id.btn_add_image)
        progressBar = findViewById(R.id.progress_bar)
        recyclerViewImages = findViewById(R.id.recycler_view_images)
        tvPostTitle = findViewById(R.id.tv_post_title)

        // Configuration des spinners
        setupCategorySpinner()
        setupRegionSpinner()

        // Configuration du RecyclerView pour les images
        recyclerViewImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewImages.adapter = DonationImagesAdapter(selectedImages)
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Nourriture",
            "Habillement",
            "Médicaments",
            "Équipements",
            "Argent",
            "Autre"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupRegionSpinner() {
        val regions = arrayOf(
            "Tunis",
            "Ariana",
            "Ben Arous",
            "Manouba",
            "Nabeul",
            "Sousse",
            "Sfax",
            "Autre"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, regions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRegion.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSubmit.setOnClickListener {
            createDonation()
        }

        btnAddImage.setOnClickListener {
            // Ouvrir la galerie pour sélectionner des images
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, PICK_IMAGES_REQUEST)
        }
    }

    private fun loadIntentData() {
        postId = intent.getStringExtra("POST_ID")
        postTitle = intent.getStringExtra("POST_TITLE")

        postTitle?.let {
            tvPostTitle.text = "Donation pour: $it"
        }
    }

    private fun createDonation() {
        val category = spinnerCategory.selectedItem.toString()
        val region = spinnerRegion.selectedItem.toString()
        val details = etDetails.text.toString().trim()

        if (details.isEmpty()) {
            etDetails.error = "Veuillez décrire votre donation"
            return
        }

        if (postId == null) {
            Toast.makeText(this, "Erreur: Post non spécifié", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = sessionManager.fetchAuthToken()
                val donorId = sessionManager.getUsername()

                if (token == null || donorId == null) {
                    withContext(Dispatchers.Main) {
                        showError("Session expirée")
                        redirectToLogin()
                    }
                    return@launch
                }

                val donationRequest = DonationRequest(
                    postId = postId,
                    donorId = donorId,
                    categorie = category.toDonationCategory(),
                    region = region,
                    details = details,
                    images = selectedImages
                )

                val response = RetrofitClient.instance.createDonation("Bearer $token", donationRequest)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        showSuccess("Donation créée avec succès!")
                        finish()
                    } else {
                        Log.e("CREATE_DONATION", "Erreur: ${response.code()} - ${response.errorBody()?.string()}")
                        showError("Erreur lors de la création: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Log.e("CREATE_DONATION", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Erreur réseau: ${e.message}")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK) {
            data?.clipData?.let { clipData ->
                // Multiple images selected
                for (i in 0 until clipData.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    selectedImages.add(imageUri.toString())
                }
            } ?: run {
                // Single image selected
                data?.data?.let { imageUri ->
                    selectedImages.add(imageUri.toString())
                }
            }

            recyclerViewImages.adapter?.notifyDataSetChanged()
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSubmit.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val PICK_IMAGES_REQUEST = 1001
    }
}

// Extension pour convertir le texte en format backend
private fun String.toDonationCategory(): String {
    return when (this) {
        "Nourriture" -> "NOURRITURE"
        "Habillement" -> "HABILLEMENT"
        "Médicaments" -> "MEDICAMENTS"
        "Équipements" -> "EQUIPEMENTS"
        "Argent" -> "ARGENT"
        else -> "AUTRE"
    }
}