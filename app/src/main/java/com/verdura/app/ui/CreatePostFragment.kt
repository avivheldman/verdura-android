package com.verdura.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.util.ImageCompressor
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch
import java.io.File

class CreatePostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private lateinit var selectedImage: ImageView
    private lateinit var addImagePlaceholder: LinearLayout
    private lateinit var removeImageButton: ImageButton
    private lateinit var postTextInput: TextInputEditText
    private lateinit var submitButton: MaterialButton
    private lateinit var submitProgress: CircularProgressIndicator
    private lateinit var imageCard: View

    private lateinit var imageCompressor: ImageCompressor

    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { handleSelectedImage(it) }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { handleSelectedImage(it) }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageCompressor = ImageCompressor(requireContext())

        imageCard = view.findViewById(R.id.imageCard)
        selectedImage = view.findViewById(R.id.selectedImage)
        addImagePlaceholder = view.findViewById(R.id.addImagePlaceholder)
        removeImageButton = view.findViewById(R.id.removeImageButton)
        postTextInput = view.findViewById(R.id.postTextInput)
        submitButton = view.findViewById(R.id.submitButton)
        submitProgress = view.findViewById(R.id.submitProgress)

        setupImagePicker()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupImagePicker() {
        imageCard.setOnClickListener { showImagePickerDialog() }

        removeImageButton.setOnClickListener {
            selectedImageUri = null
            selectedImage.visibility = View.GONE
            addImagePlaceholder.visibility = View.VISIBLE
            removeImageButton.visibility = View.GONE
        }
    }

    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Add Photo")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun launchCamera() {
        val photoFile = File(requireContext().cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }

    private fun handleSelectedImage(uri: Uri) {
        val compressed = imageCompressor.compressImage(uri)
        val finalUri = if (compressed != null) Uri.fromFile(compressed) else uri
        selectedImageUri = finalUri
        showSelectedImage(finalUri)
    }

    private fun showSelectedImage(uri: Uri) {
        selectedImage.visibility = View.VISIBLE
        addImagePlaceholder.visibility = View.GONE
        removeImageButton.visibility = View.VISIBLE
        Picasso.get().load(uri).into(selectedImage)
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            if (selectedImageUri == null) {
                Snackbar.make(requireView(), "Please add a photo of your plant", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val text = postTextInput.text?.toString()?.trim()
            if (text.isNullOrEmpty()) {
                Snackbar.make(requireView(), "Please enter some text", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Snackbar.make(requireView(), "Please sign in to create a post", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            submitButton.isEnabled = false
            submitProgress.visibility = View.VISIBLE
            viewModel.createPost(
                userId = userId,
                text = text,
                imageUri = selectedImageUri
            )
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state.postCreated) {
                        viewModel.resetPostCreated()
                        Snackbar.make(requireView(), "Post created!", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    state.error?.let { error ->
                        submitButton.isEnabled = true
                        submitProgress.visibility = View.GONE
                        Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}
