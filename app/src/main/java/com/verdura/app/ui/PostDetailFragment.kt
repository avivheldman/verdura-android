package com.verdura.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.Post
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val args: PostDetailFragmentArgs by navArgs()

    private lateinit var postImage: ImageView
    private lateinit var postText: TextView
    private lateinit var locationContainer: LinearLayout
    private lateinit var locationText: TextView
    private lateinit var timestampText: TextView
    private lateinit var actionsContainer: LinearLayout
    private lateinit var editButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var loadingIndicator: ProgressBar

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

        postImage = view.findViewById(R.id.postImage)
        postText = view.findViewById(R.id.postText)
        locationContainer = view.findViewById(R.id.locationContainer)
        locationText = view.findViewById(R.id.locationText)
        timestampText = view.findViewById(R.id.timestampText)
        actionsContainer = view.findViewById(R.id.actionsContainer)
        editButton = view.findViewById(R.id.editButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)

        setupButtons()
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

    private fun displayPost(post: Post) {
        currentPost = post

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
            locationContainer.visibility = View.VISIBLE
            locationText.text = String.format("%.4f, %.4f", post.latitude, post.longitude)
        } else {
            locationContainer.visibility = View.GONE
        }

        timestampText.text = "Created ${DateUtils.getRelativeTimeSpanString(
            post.createdAt,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )}"

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        actionsContainer.visibility = if (post.userId == currentUserId) View.VISIBLE else View.GONE
    }
}
