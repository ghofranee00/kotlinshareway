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
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnNotifications: ImageButton
    private lateinit var btnProfile: ImageButton
    private lateinit var btnLogout: ImageButton

    private lateinit var postsAdapter: DonorPostAdapter
    private var postsList = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donor_home)

        sessionManager = SessionManager(this)

        // Vérification d'authentification
        checkAuthentication()

        initializeViews()
        setupRecyclerView()
        setupAppBarListeners()
        setupNavigation()
        loadPosts()
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
        progressBar = findViewById(R.id.progress_bar)
        tvEmpty = findViewById(R.id.tv_empty)
        recyclerView = findViewById(R.id.recycler_view_posts)
        btnNotifications = findViewById(R.id.btn_notifications)
        btnProfile = findViewById(R.id.btn_profile)
        btnLogout = findViewById(R.id.btn_logout)
    }

    private fun setupRecyclerView() {
        postsAdapter = DonorPostAdapter(postsList, object : DonorPostAdapter.PostClickListener {
            override fun onHelpClick(post: Post) {
                showHelpDialog(post)
            }

            override fun onLikeClick(post: Post) {
                likePost(post)
            }

            override fun onCommentClick(post: Post) {
                showComments(post)
            }

            override fun onShareClick(post: Post) {
                sharePost(post)
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = postsAdapter
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
            loadPosts()
        }

        findViewById<Button>(R.id.nav_about).setOnClickListener {
            val intent = Intent(this, AboutUsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPosts() {
        showLoading(true)
        tvEmpty.visibility = View.GONE

        val token = sessionManager.fetchAuthToken()

        if (token == null) {
            showError("Authentication required")
            redirectToLogin()
            return
        }

        Log.d("DONOR_POSTS", "Loading posts with token: ${token.take(20)}...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getAllPosts(token)

                withContext(Dispatchers.Main) {
                    showLoading(false)

                    if (response.isSuccessful) {
                        val posts = response.body() ?: emptyList()
                        Log.d("DONOR_POSTS", "Posts loaded successfully: ${posts.size}")

                        postsList.clear()
                        postsList.addAll(posts)
                        postsAdapter.updatePosts(posts)

                        if (posts.isEmpty()) {
                            tvEmpty.visibility = View.VISIBLE
                            tvEmpty.text = "No posts available"
                        } else {
                            tvEmpty.visibility = View.GONE
                        }
                    } else {
                        Log.e("DONOR_POSTS", "HTTP Error: ${response.code()} - ${response.message()}")

                        when (response.code()) {
                            401 -> {
                                showError("Session expired - Please login again")
                                sessionManager.clearAuth()
                                redirectToLogin()
                            }
                            403 -> showError("Access denied")
                            500 -> showError("Server error")
                            else -> showError("Failed to load posts: ${response.code()}")
                        }

                        tvEmpty.visibility = View.VISIBLE
                        tvEmpty.text = "Error: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                Log.e("DONOR_POSTS", "Network error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Network error: ${e.message}")
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Network error"
                }
            }
        }
    }

    private fun showHelpDialog(post: Post) {
        AlertDialog.Builder(this)
            .setTitle("I Want to Help")
            .setMessage("Do you want to help with this ${post.typeDemande} request?\n\n${post.contenu}")
            .setPositiveButton("Yes, I Can Help") { dialog, which ->
                Toast.makeText(this, "Help functionality coming soon!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun likePost(post: Post) {
        val token = sessionManager.fetchAuthToken()
        val postId = post.getId()

        if (token == null || postId == null) {
            showError("Authentication error")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.likePost(token, postId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        loadPosts()
                        Toast.makeText(this@DonorHomeActivity, "Post liked!", Toast.LENGTH_SHORT).show()
                    } else {
                        if (response.code() == 401) {
                            showError("Session expired")
                            redirectToLogin()
                        } else {
                            showError("Failed to like post: ${response.code()}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("DONOR_LIKE", "Like error: ${e.message}")
                showError("Like failed: ${e.message}")
            }
        }
    }

    private fun showComments(post: Post) {
        Toast.makeText(this, "Comments feature coming soon!", Toast.LENGTH_SHORT).show()
    }

    private fun sharePost(post: Post) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT,
                "Check out this ${post.typeDemande} request: ${post.contenu}\n\nShared via Charity App"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share Post"))
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performLogout() {
        sessionManager.clearAuth()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        loadPosts()
    }
}