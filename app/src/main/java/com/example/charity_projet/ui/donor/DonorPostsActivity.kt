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
import com.example.charity_projet.ui.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DonorPostsActivity : AppCompatActivity(), DonorPostAdapter.PostClickListener {

    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DonorPostAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var btnRefresh: Button
    private lateinit var btnBack: ImageButton
    private lateinit var spinnerFilter: Spinner

    private var allPosts = mutableListOf<Post>()
    private var filteredPosts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_posts)

        sessionManager = SessionManager(this)
        setupViews()
        setupSpinner()
        loadAllPosts()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recycler_posts)
        progressBar = findViewById(R.id.progress_bar)
        tvEmpty = findViewById(R.id.tv_empty)
        btnRefresh = findViewById(R.id.btn_refresh)
        btnBack = findViewById(R.id.btn_back)
        spinnerFilter = findViewById(R.id.spinner_filter)

        // Setup RecyclerView avec le nouvel adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = DonorPostAdapter(emptyList(), this)
        recyclerView.adapter = adapter

        // Listeners
        btnBack.setOnClickListener { finish() }
        btnRefresh.setOnClickListener { loadAllPosts() }
    }

    private fun setupSpinner() {
        val filterOptions = arrayOf(
            "Tous les types",
            "SANTE",
            "ARGENT",
            "EDUCATION",
            "NOURRITURE",
            "VETEMENT",
            "LOGEMENT"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                applyTypeFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun applyTypeFilter() {
        val selectedType = spinnerFilter.selectedItem.toString()

        filteredPosts = if (selectedType == "Tous les types") {
            allPosts
        } else {
            allPosts.filter { post ->
                post.typeDemande == selectedType
            }.toMutableList()
        }

        showPosts(filteredPosts)
    }

    private fun loadAllPosts() {
        val token = sessionManager.fetchAuthToken()

        if (token == null) {
            showToast("Non connecté")
            redirectToLogin()
            return
        }

        showLoading(true)
        tvEmpty.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<List<Post>> =
                    RetrofitClient.instance.getAllPosts("Bearer $token")

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val posts = response.body() ?: emptyList()
                        allPosts = posts.toMutableList()
                        applyTypeFilter()
                        showToast("${posts.size} posts chargés")
                    } else {
                        when (response.code()) {
                            401 -> {
                                showToast("Session expirée")
                                redirectToLogin()
                            }

                            403 -> showToast("Accès refusé")
                            else -> showToast("Erreur: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showToast("Erreur réseau: ${e.message}")
                    showEmptyState(true, "Erreur de connexion")
                }
            }
        }
    }

    private fun showPosts(posts: List<Post>) {
        adapter.updatePosts(posts)
        if (posts.isEmpty()) {
            val selectedType = spinnerFilter.selectedItem.toString()
            val message = if (selectedType == "Tous les types") {
                "Aucun post disponible"
            } else {
                "Aucun post de type $selectedType"
            }
            showEmptyState(true, message)
        } else {
            showEmptyState(false, "")
        }
    }

    private fun showEmptyState(show: Boolean, message: String) {
        if (show) {
            tvEmpty.text = message
            tvEmpty.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnRefresh.isEnabled = !show
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    companion object {
        private const val REQUEST_CODE_CREATE_DONATION = 1001
    }

    override fun onHelpClick(post: Post) {
        // Récupérer l'ID du post - handle null case
        val postId = post.getId() ?: run {
            Log.e("DonorPosts", "Post ID is null! Post: $post")
            Toast.makeText(this, "Error: Invalid post", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("DonorPosts", "onHelpClick - Post ID: $postId")
        Log.d("DonorPosts", "Post type: ${post.typeDemande}")
        Log.d("DonorPosts", "Post content length: ${post.contenu?.length ?: 0}")

        AlertDialog.Builder(this)
            .setTitle("Help - ${post.typeDemande ?: "Request"}")
            .setMessage("Do you want to help with this request?\n\n${post.contenu ?: "No content"}")
            .setPositiveButton("Yes, I want to help") { dialog, which ->
                // Passer les données
                val intent = Intent(this, CreateDonationActivity::class.java).apply {
                    putExtra("POST_ID", postId)
                    putExtra("POST_CONTENT", post.contenu ?: "")
                    putExtra("POST_TYPE", post.typeDemande ?: "GENERAL")
                }

                Log.d("DonorPosts", "Starting CreateDonationActivity with:")
                Log.d("DonorPosts", "POST_ID: $postId")
                Log.d("DonorPosts", "POST_CONTENT: ${post.contenu?.take(50)}...")
                Log.d("DonorPosts", "POST_TYPE: ${post.typeDemande}")

                startActivityForResult(intent, REQUEST_CODE_CREATE_DONATION)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }






    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE_DONATION && resultCode == RESULT_OK) {
            Toast.makeText(this, "Donation created successfully!", Toast.LENGTH_SHORT).show()
            // Optionally refresh posts
            loadAllPosts()
        }}
    override fun onShareClick(post: Post) {
        val shareText = """
            📢 Post Charity App
            Type: ${post.typeDemande}
            Contenu: ${post.contenu}
            Date: ${formatDate(post.dateCreation)}
            
            Rejoignez-nous pour aider !
        """.trimIndent()

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "Post Charity App - ${post.typeDemande}")
        }
        startActivity(Intent.createChooser(shareIntent, "Partager ce post"))
    }

    private fun performHelpAction(post: Post) {
        showToast("Merci ! Votre aide pour ${post.typeDemande} a été enregistrée")
        // Implémentez l'appel API pour enregistrer l'aide
    }

    private fun formatDate(date: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val dateObj = inputFormat.parse(date ?: "")
            outputFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            date ?: "Date inconnue"
        }
    }
}