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
        fun onLikeClick(post: Post)
        fun onCommentClick(post: Post)
        fun onShareClick(post: Post)
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        val btnLike: ImageButton = itemView.findViewById(R.id.btn_like)
        val btnComment: ImageButton = itemView.findViewById(R.id.btn_comment)
        val btnShare: ImageButton = itemView.findViewById(R.id.btn_share)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.tvUsername.text = post.user?.firstName ?: "Anonymous"
        holder.tvDate.text = formatDate(post.dateCreation)
        holder.tvType.text = post.typeDemande ?: "General"
        holder.tvContent.text = post.contenu ?: "No content"
        holder.tvLikesCount.text = "Likes: ${post.likesCount}"
        holder.tvCommentsCount.text = "Comments: ${post.commentsCount}"

        // Set button colors based on demand type
        when (post.typeDemande?.uppercase()) {
            "NOURRITURE" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#4CAF50"))
            "LOGEMENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#2196F3"))
            "SANTE" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#F44336"))
            "EDUCATION" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#FF9800"))
            "VETEMENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#9C27B0"))
            "ARGENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#FFC107"))
            else -> holder.btnHelp.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.purple_500))
        }

        // Images
        if (post.imageUrls.isNotEmpty()) {
            holder.ivPostImage.visibility = View.VISIBLE
            holder.tvImageCount.visibility = View.VISIBLE
            if (post.imageUrls.size > 1) {
                holder.tvImageCount.text = "+${post.imageUrls.size - 1}"
            } else {
                holder.tvImageCount.visibility = View.GONE
            }
        } else {
            holder.ivPostImage.visibility = View.GONE
            holder.tvImageCount.visibility = View.GONE
        }

        // Videos
        if (post.videoUrls.isNotEmpty()) {
            holder.tvVideoCount.visibility = View.VISIBLE
            holder.tvVideoCount.text = "📹 ${post.videoUrls.size} video(s)"
        } else {
            holder.tvVideoCount.visibility = View.GONE
        }

        // Afficher les boutons pour le donor
        holder.buttonsLayout.visibility = View.VISIBLE

        // Set click listeners
        holder.btnHelp.setOnClickListener {
            listener.onHelpClick(post)
        }

        holder.btnLike.setOnClickListener {
            listener.onLikeClick(post)
        }

        holder.btnComment.setOnClickListener {
            listener.onCommentClick(post)
        }

        holder.btnShare.setOnClickListener {
            listener.onShareClick(post)
        }
    }

    override fun getItemCount(): Int = posts.size

    private fun formatDate(date: String?): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
            val dateObj = inputFormat.parse(date ?: "")
            outputFormat.format(dateObj ?: Date())
        } catch (e: Exception) {
            try {
                date?.substring(0, 10) ?: "Unknown date"
            } catch (e2: Exception) {
                "Unknown date"
            }
        }
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }
}