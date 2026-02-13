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
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.Post

class PostAdapter(
    private val onPostClick: (Post) -> Unit,
    private val onPostLongClick: (Post) -> Boolean = { false }
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val postText: TextView = itemView.findViewById(R.id.postText)
        private val locationIcon: ImageView = itemView.findViewById(R.id.locationIcon)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(post: Post) {
            postText.text = post.text

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
            itemView.setOnLongClickListener { onPostLongClick(post) }
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
