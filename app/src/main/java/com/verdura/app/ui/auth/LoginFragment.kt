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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.verdura.app.R
import com.verdura.app.databinding.FragmentLoginBinding
import com.verdura.app.repository.FirebaseAuthRepository
import com.verdura.app.util.FormValidator
import com.verdura.app.viewmodel.AuthState
import com.verdura.app.viewmodel.AuthViewModel
import com.verdura.app.viewmodel.AuthViewModelFactory

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels { AuthViewModelFactory(FirebaseAuthRepository()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        binding.emailEditText.doAfterTextChanged { binding.emailInputLayout.error = null }
        binding.passwordEditText.doAfterTextChanged { binding.passwordInputLayout.error = null }
        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.forgotPasswordTextView.setOnClickListener { showForgotPasswordDialog() }
        binding.registerPromptTextView.setOnClickListener { navigateToRegister() }
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
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isFailure) showError(it.exceptionOrNull()?.message ?: getString(R.string.error_login_failed))
                authViewModel.clearLoginResult()
            }
        }
        authViewModel.resetPasswordResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) Toast.makeText(context, R.string.password_reset_sent, Toast.LENGTH_LONG).show()
                else showError(it.exceptionOrNull()?.message ?: "Failed to send reset email")
                authViewModel.clearResetPasswordResult()
            }
        }
    }

    private fun attemptLogin() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString()
        var isValid = true
        FormValidator.validateEmail(email).let { if (!it.isValid) { binding.emailInputLayout.error = it.errorMessage; isValid = false } }
        FormValidator.validatePassword(password).let { if (!it.isValid) { binding.passwordInputLayout.error = it.errorMessage; isValid = false } }
        if (isValid) authViewModel.login(email, password)
    }

    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_forgot_password, null)
        val emailEditText = dialogView.findViewById<TextInputEditText>(R.id.emailEditText)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.forgot_password)
            .setView(dialogView)
            .setPositiveButton(R.string.send) { _, _ ->
                val email = emailEditText.text.toString().trim()
                if (FormValidator.isValidEmail(email)) authViewModel.resetPassword(email)
                else Toast.makeText(context, R.string.error_email_invalid, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun navigateToRegister() {
        parentFragmentManager.beginTransaction().replace(R.id.authContainer, RegisterFragment()).addToBackStack(null).commit()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loginButton.isEnabled = !isLoading
        binding.loginButton.text = if (isLoading) "" else getString(R.string.sign_in)
        binding.progressIndicator.isVisible = isLoading
    }

    private fun showError(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
