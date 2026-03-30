package com.verdura.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var authorImage: ImageView
    private lateinit var authorName: TextView
    private lateinit var postImage: ImageView
    private lateinit var postText: TextView
    private lateinit var locationContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var timestampText: TextView
    private lateinit var actionsContainer: LinearLayout
    private lateinit var editButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var likeButton: ImageView
    private lateinit var likeCountText: TextView
    private lateinit var commentsHeader: TextView
    private lateinit var commentsContainer: LinearLayout
    private lateinit var noCommentsText: TextView
    private lateinit var commentInput: TextInputEditText
    private lateinit var sendCommentButton: MaterialButton

    private val firestore = FirebaseFirestore.getInstance()
    private var currentPost: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authorImage = view.findViewById(R.id.authorImage)
        authorName = view.findViewById(R.id.authorName)
        postImage = view.findViewById(R.id.postImage)
        postText = view.findViewById(R.id.postText)
        locationContainer = view.findViewById(R.id.locationContainer)
        locationText = view.findViewById(R.id.locationText)
        timestampText = view.findViewById(R.id.timestampText)
        actionsContainer = view.findViewById(R.id.actionsContainer)
        editButton = view.findViewById(R.id.editButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        likeButton = view.findViewById(R.id.likeButton)
        likeCountText = view.findViewById(R.id.likeCountText)
        commentsHeader = view.findViewById(R.id.commentsHeader)
        commentsContainer = view.findViewById(R.id.commentsContainer)
        noCommentsText = view.findViewById(R.id.noCommentsText)
        commentInput = view.findViewById(R.id.commentInput)
        sendCommentButton = view.findViewById(R.id.sendCommentButton)

        setupButtons()
        setupLikeButton()
        setupCommentInput()
        observeViewModel()
        viewModel.loadPostById(args.postId)
    }

    private fun setupButtons() {
        editButton.setOnClickListener {
            currentPost?.let { post ->
                val action = PostDetailFragmentDirections.actionPostDetailToEditPost(post.id)
                findNavController().navigate(action)
            }
        }

        deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                currentPost?.let { post ->
                    viewModel.deletePost(post.id)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedPost.collect { post ->
                        post?.let { displayPost(it) }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                        if (state.postDeleted) {
                            viewModel.resetPostDeleted()
                            Snackbar.make(requireView(), "Post deleted", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }

                        state.error?.let { error ->
                            Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun setupLikeButton() {
        likeButton.setOnClickListener {
            val post = currentPost ?: return@setOnClickListener
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val isLiked = post.likedBy.contains(userId)
            viewModel.toggleLike(post.id, userId, isLiked)
        }
    }

    private fun setupCommentInput() {
        sendCommentButton.setOnClickListener {
            val post = currentPost ?: return@setOnClickListener
            val user = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener
            val text = commentInput.text?.toString()?.trim()
            if (text.isNullOrEmpty()) {
                Snackbar.make(requireView(), "Please enter a comment", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addComment(
                postId = post.id,
                userId = user.uid,
                userName = user.displayName ?: "Anonymous",
                text = text
            )
            commentInput.text?.clear()
        }
    }

    private fun displayPost(post: Post) {
        currentPost = post

        postText.text = post.text
        loadAuthor(post.userId)
        displayLikes(post)
        displayComments(post.comments)

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
            locationContainer.visibility = View.VISIBLE
            locationText.text = String.format("%.4f, %.4f", post.latitude, post.longitude)
        } else {
            locationContainer.visibility = View.GONE
        }

        timestampText.text = DateUtils.getRelativeTimeSpanString(
            post.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        actionsContainer.visibility = if (post.userId == currentUserId) View.VISIBLE else View.GONE
    }

    private fun displayLikes(post: Post) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
        likeButton.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        )
        val count = post.likedBy.size
        likeCountText.text = if (count == 1) "1 like" else "$count likes"
    }

    private fun displayComments(comments: List<Comment>) {
        commentsHeader.text = "Comments (${comments.size})"
        commentsContainer.removeAllViews()

        if (comments.isEmpty()) {
            noCommentsText.visibility = View.VISIBLE
            return
        }
        noCommentsText.visibility = View.GONE

        for (comment in comments.sortedByDescending { it.timestamp }) {
            val commentView = LayoutInflater.from(requireContext())
                .inflate(android.R.layout.simple_list_item_2, commentsContainer, false)

            val nameView = commentView.findViewById<TextView>(android.R.id.text1)
            val textView = commentView.findViewById<TextView>(android.R.id.text2)

            nameView.text = comment.userName
            nameView.textSize = 13f
            nameView.setTextColor(resources.getColor(android.R.color.black, null))

            textView.text = comment.text
            textView.textSize = 14f

            commentsContainer.addView(commentView)
        }
    }

    private fun loadAuthor(userId: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && currentUser.uid == userId) {
            authorName.text = currentUser.displayName ?: "You"
            currentUser.photoUrl?.let { url ->
                Glide.with(this)
                    .load(url.toString())
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(authorImage)
            } ?: Glide.with(this)
                .load(R.drawable.ic_person)
                .circleCrop()
                .into(authorImage)
            return
        }

        authorName.text = ""
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                authorName.text = doc.getString("displayName") ?: "Unknown"
                val photoUrl = doc.getString("photoUrl")
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_person)
                        .into(authorImage)
                } else {
                    Glide.with(this)
                        .load(R.drawable.ic_person)
                        .circleCrop()
                        .into(authorImage)
                }
            }
    }
}
