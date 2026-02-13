package com.verdura.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.model.Post
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class EditPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val args: EditPostFragmentArgs by navArgs()

    private lateinit var postImage: ImageView
    private lateinit var postTextInput: TextInputEditText
    private lateinit var locationText: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var saveProgress: ProgressBar

    private var currentPost: Post? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postImage = view.findViewById(R.id.postImage)
        postTextInput = view.findViewById(R.id.postTextInput)
        locationText = view.findViewById(R.id.locationText)
        saveButton = view.findViewById(R.id.saveButton)
        saveProgress = view.findViewById(R.id.saveProgress)

        setupSaveButton()
        observeViewModel()
        viewModel.loadPostById(args.postId)
    }

    private fun setupSaveButton() {
        saveButton.setOnClickListener {
            val text = postTextInput.text?.toString()?.trim()

            if (text.isNullOrEmpty()) {
                Snackbar.make(requireView(), "Please enter some text", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            currentPost?.let { post ->
                saveButton.isEnabled = false
                saveProgress.visibility = View.VISIBLE
                viewModel.updatePost(post.copy(text = text))
            }
        }
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
                        if (state.postUpdated) {
                            viewModel.resetPostUpdated()
                            Snackbar.make(requireView(), "Post updated!", Snackbar.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }

                        state.error?.let { error ->
                            saveButton.isEnabled = true
                            saveProgress.visibility = View.GONE
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

        postTextInput.setText(post.text)

        if (!post.imageUrl.isNullOrEmpty()) {
            postImage.visibility = View.VISIBLE
            Picasso.get()
                .load(post.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(postImage)
        } else {
            postImage.visibility = View.GONE
        }

        if (post.latitude != null && post.longitude != null) {
            locationText.visibility = View.VISIBLE
            locationText.text = String.format("Location: %.4f, %.4f", post.latitude, post.longitude)
        } else {
            locationText.visibility = View.GONE
        }
    }
}
