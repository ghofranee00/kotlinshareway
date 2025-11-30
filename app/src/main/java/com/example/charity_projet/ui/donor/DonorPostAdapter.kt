package com.example.charity_projet.ui.donor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.charity_projet.R
import com.example.charity_projet.models.Post
import java.text.SimpleDateFormat
import java.util.*

class DonorPostAdapter(
    private var posts: List<Post>,
    private val listener: PostClickListener
) : RecyclerView.Adapter<DonorPostAdapter.PostViewHolder>() {

    interface PostClickListener {
        fun onHelpClick(post: Post)
        fun onShareClick(post: Post)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Tous ces IDs existent dans le nouveau layout item_donor_post.xml
        val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        val tvDate: TextView = itemView.findViewById(R.id.tv_date)
        val tvType: TextView = itemView.findViewById(R.id.tv_type)
        val tvContent: TextView = itemView.findViewById(R.id.tv_content)
        val tvLikesCount: TextView = itemView.findViewById(R.id.tv_likes_count)
        val tvCommentsCount: TextView = itemView.findViewById(R.id.tv_comments_count)
        val ivPostImage: ImageView = itemView.findViewById(R.id.iv_post_image)
        val tvImageCount: TextView = itemView.findViewById(R.id.tv_image_count)
        val tvVideoCount: TextView = itemView.findViewById(R.id.tv_video_count)
        val buttonsLayout: LinearLayout = itemView.findViewById(R.id.buttons_layout)
        val btnHelp: Button = itemView.findViewById(R.id.btn_help)
        val btnShare: ImageButton = itemView.findViewById(R.id.btn_share)

        // Pas de btnLike et btnComment dans le nouveau layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donor_post, parent, false) // Utilise le nouveau layout
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        // Setup des données de base
        holder.tvUsername.text = post.user?.firstName ?: post.user?.identifiant ?: "Anonymous"
        holder.tvDate.text = formatDate(post.dateCreation)
        holder.tvType.text = post.typeDemande ?: "General"
        holder.tvContent.text = post.contenu ?: "No content"
        holder.tvLikesCount.text = "Likes: ${post.likesCount}"
        holder.tvCommentsCount.text = "Comments: ${post.commentsCount}"

        // Couleur du bouton Help selon le type
        setupHelpButtonColor(holder.btnHelp, post.typeDemande)

        // Gestion des médias
        setupMedia(holder, post)

        // Listeners
        holder.btnHelp.setOnClickListener {
            listener.onHelpClick(post)
        }

        holder.btnShare.setOnClickListener {
            listener.onShareClick(post)
        }
    }

    private fun setupHelpButtonColor(button: Button, type: String?) {
        when (type?.uppercase()) {
            "NOURRITURE" -> button.setBackgroundColor(Color.parseColor("#4CAF50"))
            "LOGEMENT" -> button.setBackgroundColor(Color.parseColor("#2196F3"))
            "SANTE" -> button.setBackgroundColor(Color.parseColor("#F44336"))
            "EDUCATION" -> button.setBackgroundColor(Color.parseColor("#FF9800"))
            "VETEMENT" -> button.setBackgroundColor(Color.parseColor("#9C27B0"))
            "ARGENT" -> button.setBackgroundColor(Color.parseColor("#FFC107"))
            else -> button.setBackgroundColor(ContextCompat.getColor(button.context, R.color.purple_500))
        }
    }

    private fun setupMedia(holder: PostViewHolder, post: Post) {
        // Images
        if (!post.imageUrls.isNullOrEmpty()) {
            holder.ivPostImage.visibility = View.VISIBLE
            if (post.imageUrls.size > 1) {
                holder.tvImageCount.visibility = View.VISIBLE
                holder.tvImageCount.text = "+${post.imageUrls.size - 1}"
            } else {
                holder.tvImageCount.visibility = View.GONE
            }
            // Ici vous pouvez charger l'image avec Glide/Picasso
        } else {
            holder.ivPostImage.visibility = View.GONE
            holder.tvImageCount.visibility = View.GONE
        }

        // Videos
        if (!post.videoUrls.isNullOrEmpty()) {
            holder.tvVideoCount.visibility = View.VISIBLE
            holder.tvVideoCount.text = "📹 ${post.videoUrls.size} video(s)"
        } else {
            holder.tvVideoCount.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = posts.size

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    private fun formatDate(date: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val dateObj = inputFormat.parse(date ?: "")
            outputFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            try {
                date?.substring(0, 10) ?: "Date inconnue"
            } catch (e2: Exception) {
                "Date inconnue"
            }
        }
    }
}