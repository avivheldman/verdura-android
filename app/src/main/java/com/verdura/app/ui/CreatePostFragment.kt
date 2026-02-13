package com.verdura.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import com.verdura.app.R
import com.verdura.app.viewmodel.PostViewModel
import kotlinx.coroutines.launch

class CreatePostFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()

    private lateinit var selectedImage: ImageView
    private lateinit var addImagePlaceholder: LinearLayout
    private lateinit var removeImageButton: ImageButton
    private lateinit var postTextInput: TextInputEditText
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var locationText: TextView
    private lateinit var locationProgress: ProgressBar
    private lateinit var submitButton: MaterialButton
    private lateinit var submitProgress: ProgressBar
    private lateinit var imageCard: View

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedImageUri: Uri? = null
    private var currentLocation: Location? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            showSelectedImage(it)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineLocationGranted || coarseLocationGranted) {
            fetchCurrentLocation()
        } else {
            locationSwitch.isChecked = false
            Snackbar.make(requireView(), "Location permission denied", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_post, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        imageCard = view.findViewById(R.id.imageCard)
        selectedImage = view.findViewById(R.id.selectedImage)
        addImagePlaceholder = view.findViewById(R.id.addImagePlaceholder)
        removeImageButton = view.findViewById(R.id.removeImageButton)
        postTextInput = view.findViewById(R.id.postTextInput)
        locationSwitch = view.findViewById(R.id.locationSwitch)
        locationText = view.findViewById(R.id.locationText)
        locationProgress = view.findViewById(R.id.locationProgress)
        submitButton = view.findViewById(R.id.submitButton)
        submitProgress = view.findViewById(R.id.submitProgress)

        setupImagePicker()
        setupLocationSwitch()
        setupSubmitButton()
        observeViewModel()
    }

    private fun setupImagePicker() {
        imageCard.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        removeImageButton.setOnClickListener {
            selectedImageUri = null
            selectedImage.visibility = View.GONE
            addImagePlaceholder.visibility = View.VISIBLE
            removeImageButton.visibility = View.GONE
        }
    }

    private fun showSelectedImage(uri: Uri) {
        selectedImage.visibility = View.VISIBLE
        addImagePlaceholder.visibility = View.GONE
        removeImageButton.visibility = View.VISIBLE
        Picasso.get().load(uri).into(selectedImage)
    }

    private fun setupLocationSwitch() {
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestLocationPermission()
            } else {
                currentLocation = null
                locationText.visibility = View.GONE
            }
        }
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                fetchCurrentLocation()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun fetchCurrentLocation() {
        locationProgress.visibility = View.VISIBLE
        locationText.visibility = View.GONE

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                locationProgress.visibility = View.GONE
                if (location != null) {
                    currentLocation = location
                    locationText.visibility = View.VISIBLE
                    locationText.text = String.format(
                        "Location: %.4f, %.4f",
                        location.latitude,
                        location.longitude
                    )
                } else {
                    Snackbar.make(requireView(), "Could not get location", Snackbar.LENGTH_SHORT).show()
                    locationSwitch.isChecked = false
                }
            }.addOnFailureListener {
                locationProgress.visibility = View.GONE
                Snackbar.make(requireView(), "Error getting location", Snackbar.LENGTH_SHORT).show()
                locationSwitch.isChecked = false
            }
        } catch (e: SecurityException) {
            locationProgress.visibility = View.GONE
            locationSwitch.isChecked = false
        }
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
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
                imageUrl = selectedImageUri?.toString(),
                latitude = currentLocation?.latitude,
                longitude = currentLocation?.longitude
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
