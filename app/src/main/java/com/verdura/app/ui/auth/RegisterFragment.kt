package com.verdura.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.verdura.app.R
import com.verdura.app.databinding.FragmentRegisterBinding
import com.verdura.app.repository.FirebaseAuthRepository
import com.verdura.app.util.FormValidator
import com.verdura.app.viewmodel.AuthState
import com.verdura.app.viewmodel.AuthViewModel
import com.verdura.app.viewmodel.AuthViewModelFactory

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels { AuthViewModelFactory(FirebaseAuthRepository()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.nameEditText.doAfterTextChanged { binding.nameInputLayout.error = null }
        binding.emailEditText.doAfterTextChanged { binding.emailInputLayout.error = null }
        binding.passwordEditText.doAfterTextChanged { binding.passwordInputLayout.error = null }
        binding.confirmPasswordEditText.doAfterTextChanged { binding.confirmPasswordInputLayout.error = null }
        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.loginPromptTextView.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun observeViewModel() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> setLoadingState(true)
                is AuthState.Authenticated -> setLoadingState(false)
                is AuthState.Unauthenticated -> setLoadingState(false)
                is AuthState.Error -> { setLoadingState(false); showError(state.message) }
            }
        }
        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isFailure) showError(it.exceptionOrNull()?.message ?: getString(R.string.error_register_failed))
                authViewModel.clearRegisterResult()
            }
        }
    }

    private fun attemptRegister() {
        val name = binding.nameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        val confirmPassword = binding.confirmPasswordEditText.text.toString()
        var isValid = true
        FormValidator.validateDisplayName(name).let { if (!it.isValid) { binding.nameInputLayout.error = it.errorMessage; isValid = false } }
        FormValidator.validateEmail(email).let { if (!it.isValid) { binding.emailInputLayout.error = it.errorMessage; isValid = false } }
        FormValidator.validatePassword(password).let { if (!it.isValid) { binding.passwordInputLayout.error = it.errorMessage; isValid = false } }
        FormValidator.validateConfirmPassword(password, confirmPassword).let { if (!it.isValid) { binding.confirmPasswordInputLayout.error = it.errorMessage; isValid = false } }
        if (isValid) authViewModel.register(email, password, name)
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.registerButton.isEnabled = !isLoading
        binding.registerButton.text = if (isLoading) "" else getString(R.string.sign_up)
        binding.progressIndicator.isVisible = isLoading
    }

    private fun showError(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
