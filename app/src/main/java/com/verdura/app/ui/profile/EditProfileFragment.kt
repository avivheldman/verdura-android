package com.verdura.app.ui.profile

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.verdura.app.R
import com.verdura.app.databinding.FragmentEditProfileBinding
import com.verdura.app.repository.FirebaseUserRepository
import com.verdura.app.viewmodel.ProfileViewModel
import com.verdura.app.viewmodel.ProfileViewModelFactory

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels { ProfileViewModelFactory(FirebaseUserRepository()) }
    private var selectedPhotoUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            Glide.with(this).load(it).circleCrop().into(binding.profileImageView)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        loadProfile()
    }

    private fun setupViews() {
        binding.changePhotoButton.setOnClickListener { pickImage.launch("image/*") }
        binding.saveButton.setOnClickListener { saveProfile() }
        binding.cancelButton.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.nameEditText.setText(it.displayName)
                it.photoUrl?.let { url ->
                    Glide.with(this).load(url).circleCrop().placeholder(R.drawable.ic_person).into(binding.profileImageView)
                }
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.isVisible = isLoading
            binding.saveButton.isEnabled = !isLoading
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearError() }
        }
        viewModel.updateSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, R.string.profile_updated, Toast.LENGTH_SHORT).show()
                viewModel.clearUpdateSuccess()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun loadProfile() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { viewModel.loadUser(it) }
    }

    private fun saveProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val name = binding.nameEditText.text.toString().trim()
        if (name.isBlank()) {
            binding.nameInputLayout.error = getString(R.string.error_name_required)
            return
        }
        selectedPhotoUri?.let { viewModel.updateProfilePhoto(userId, it) }
        viewModel.user.value?.copy(displayName = name)?.let { viewModel.updateUser(it) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
