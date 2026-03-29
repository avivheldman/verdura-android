package com.verdura.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
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
import com.verdura.app.util.ImageCompressor
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import java.io.File

class EditPostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val args: EditPostFragmentArgs by navArgs()

    private lateinit var postImage: ImageView
    private lateinit var postTextInput: TextInputEditText
    private lateinit var locationText: TextView
    private lateinit var saveButton: MaterialButton
    private lateinit var saveProgress: ProgressBar
    private lateinit var imageCompressor: ImageCompressor

    private var currentPost: Post? = null
    private var newImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { handleNewImage(it) } }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraImageUri?.let { handleNewImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_edit_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageCompressor = ImageCompressor(requireContext())
        postImage = view.findViewById(R.id.postImage)
        postTextInput = view.findViewById(R.id.postTextInput)
        locationText = view.findViewById(R.id.locationText)
        saveButton = view.findViewById(R.id.saveButton)
        saveProgress = view.findViewById(R.id.saveProgress)

        postImage.setOnClickListener { showImagePickerDialog() }
        setupSaveButton()
        observeViewModel()
        viewModel.loadPostById(args.postId)
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Photo")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "camera_edit_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(), "${requireContext().packageName}.fileprovider", photoFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun handleNewImage(uri: Uri) {
        val compressed = imageCompressor.compressImage(uri)
        newImageUri = if (compressed != null) Uri.fromFile(compressed) else uri
        postImage.visibility = View.VISIBLE
        Picasso.get().load(newImageUri).into(postImage)
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

                if (newImageUri != null) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val uploadResult = viewModel.uploadImage(post.id, newImageUri!!)
                        uploadResult.fold(
                            onSuccess = { downloadUrl ->
                                viewModel.updatePost(post.copy(text = text, imageUrl = downloadUrl))
                            },
                            onFailure = {
                                saveButton.isEnabled = true
                                saveProgress.visibility = View.GONE
                                Snackbar.make(requireView(), "Failed to upload image", Snackbar.LENGTH_LONG).show()
                            }
                        )
                    }
                } else {
                    viewModel.updatePost(post.copy(text = text))
                }
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
            Picasso.get().load(post.imageUrl).placeholder(android.R.drawable.ic_menu_gallery).into(postImage)
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
