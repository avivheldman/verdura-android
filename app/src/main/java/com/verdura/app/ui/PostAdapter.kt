package com.verdura.app.ui

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.Post

class PostAdapter(
    private val onPostClick: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    private data class AuthorInfo(val name: String, val photoUrl: String?)
    private val authorCache = mutableMapOf<String, AuthorInfo>()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorImage: ImageView = itemView.findViewById(R.id.authorImage)
        private val authorText: TextView = itemView.findViewById(R.id.authorText)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val postText: TextView = itemView.findViewById(R.id.postText)
        private val locationIcon: ImageView = itemView.findViewById(R.id.locationIcon)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likeCountText: TextView = itemView.findViewById(R.id.likeCountText)
        private val commentCountText: TextView = itemView.findViewById(R.id.commentCountText)

        fun bind(post: Post) {
            postText.text = post.text
            bindAuthor(post.userId)
            bindLikesAndComments(post)

            if (!post.imageUrl.isNullOrEmpty()) {
                postImage.visibility = View.VISIBLE
                Picasso.get()
                    .load(post.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(postImage)
            } else {
                postImage.visibility = View.GONE
            }

            if (post.latitude != null && post.longitude != null) {
                locationIcon.visibility = View.VISIBLE
                locationText.visibility = View.VISIBLE
                locationText.text = String.format("%.4f, %.4f", post.latitude, post.longitude)
            } else {
                locationIcon.visibility = View.GONE
                locationText.visibility = View.GONE
            }

            timestampText.text = DateUtils.getRelativeTimeSpanString(
                post.createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )

            itemView.setOnClickListener { onPostClick(post) }
        }

        private fun bindAuthor(userId: String) {
            val cached = authorCache[userId]
            if (cached != null) {
                authorText.text = cached.name
                loadAuthorPhoto(cached.photoUrl)
                return
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && currentUser.uid == userId) {
                val name = currentUser.displayName ?: "You"
                val photoUrl = currentUser.photoUrl?.toString()
                authorCache[userId] = AuthorInfo(name, photoUrl)
                authorText.text = name
                loadAuthorPhoto(photoUrl)
                return
            }

            authorText.text = ""
            loadAuthorPhoto(null)
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("displayName") ?: "Unknown"
                    val photoUrl = doc.getString("photoUrl")
                    authorCache[userId] = AuthorInfo(name, photoUrl)
                    authorText.text = name
                    loadAuthorPhoto(photoUrl)
                }
        }

        private fun loadAuthorPhoto(photoUrl: String?) {
            if (!photoUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(authorImage)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_person)
                    .circleCrop()
                    .into(authorImage)
            }
        }

        private fun bindLikesAndComments(post: Post) {
            val likeCount = post.likedBy.size
            likeCountText.text = likeCount.toString()

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
            likeIcon.setImageResource(
                if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )

            commentCountText.text = post.comments.size.toString()
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
