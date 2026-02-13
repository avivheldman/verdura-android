package com.verdura.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.verdura.app.R
import com.verdura.app.databinding.ActivityMainBinding
import com.verdura.app.repository.FirebaseAuthRepository
import com.verdura.app.ui.auth.LoginFragment
import com.verdura.app.viewmodel.AuthState
import com.verdura.app.viewmodel.AuthViewModel
import com.verdura.app.viewmodel.AuthViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(FirebaseAuthRepository()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        observeAuthState()
        if (savedInstanceState == null) checkInitialAuthState()
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthState.Loading -> binding.loadingIndicator.isVisible = true
                is AuthState.Authenticated -> { binding.loadingIndicator.isVisible = false; showMainContent() }
                is AuthState.Unauthenticated -> { binding.loadingIndicator.isVisible = false; showAuthScreen() }
                is AuthState.Error -> binding.loadingIndicator.isVisible = false
            }
        }
    }

    private fun checkInitialAuthState() {
        if (authViewModel.isLoggedIn) showMainContent() else showAuthScreen()
    }

    private fun showAuthScreen() {
        binding.authContainer.isVisible = true
        binding.mainContainer.isVisible = false
        binding.bottomNavigation.isVisible = false
        supportFragmentManager.beginTransaction().replace(R.id.authContainer, LoginFragment()).commit()
    }

    private fun showMainContent() {
        binding.authContainer.isVisible = false
        binding.mainContainer.isVisible = true
        binding.bottomNavigation.isVisible = true
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_explore -> true
                R.id.nav_profile -> true
                else -> false
            }
        }
    }
}
