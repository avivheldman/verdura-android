package com.verdura.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.verdura.app.R
import com.verdura.app.databinding.FragmentProfileBinding
import com.verdura.app.data.AppDatabase
import com.verdura.app.repository.CachingUserRepository
import com.verdura.app.repository.FirebaseUserRepository
import com.verdura.app.ui.MainActivity
import com.verdura.app.viewmodel.ProfileViewModel
import com.verdura.app.viewmodel.ProfileViewModelFactory

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels {
        val userDao = AppDatabase.getInstance(requireContext()).userDao()
        ProfileViewModelFactory(CachingUserRepository(FirebaseUserRepository(), userDao))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        loadProfile()
    }

    private fun setupViews() {
        binding.editProfileButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_editProfile)
        }
        binding.myPostsButton.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_myPosts)
        }
        binding.logoutButton.setOnClickListener {
            (requireActivity() as? MainActivity)?.authViewModel?.logout()
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.nameTextView.text = it.displayName ?: "No name"
                binding.emailTextView.text = it.email
                it.photoUrl?.let { url ->
                    Glide.with(this).load(url).circleCrop().placeholder(R.drawable.ic_person).into(binding.profileImageView)
                }
            }
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressIndicator.isVisible = isLoading
        }
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); viewModel.clearError() }
        }
    }

    private fun loadProfile() {
        FirebaseAuth.getInstance().currentUser?.uid?.let { viewModel.loadUser(it) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
