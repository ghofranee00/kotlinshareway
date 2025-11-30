package com.example.charity_projet.ui.donor

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.charity_projet.R
import com.example.charity_projet.models.Post

class PostAdapter(
    private var posts: List<Post>,
    private val listener: PostClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

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

        // DEBUG: Log chaque post
        Log.d("POST_ADAPTER_DEBUG", "Binding post: ${post.contenu?.take(20)}...")

        holder.tvUsername.text = post.user?.firstName ?: "Anonymous"
        holder.tvDate.text = formatDate(post.dateCreation)
        holder.tvType.text = post.typeDemande ?: "General"
        holder.tvContent.text = post.contenu ?: "No content"
        holder.tvLikesCount.text = "Likes: ${post.likesCount}"

        // Set button colors
        when (post.typeDemande?.uppercase()) {
            "NOURRITURE" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#4CAF50"))
            "LOGEMENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#2196F3"))
            "SANTE" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#F44336"))
            "EDUCATION" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#FF9800"))
            "VETEMENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#9C27B0"))
            "ARGENT" -> holder.btnHelp.setBackgroundColor(Color.parseColor("#FFC107"))
            else -> holder.btnHelp.setBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.purple_500))
        }

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
            date?.substring(0, 10) ?: "Unknown date"
        } catch (e: Exception) {
            "Unknown date"
        }
    }
}