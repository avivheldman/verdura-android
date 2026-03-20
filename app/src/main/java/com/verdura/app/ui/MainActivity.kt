package com.verdura.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.verdura.app.R
import com.verdura.app.data.AppDatabase
import com.verdura.app.databinding.ActivityMainBinding
import com.verdura.app.repository.FirebaseAuthRepository
import com.verdura.app.ui.auth.LoginFragment
import com.verdura.app.viewmodel.AuthState
import com.verdura.app.viewmodel.AuthViewModel
import com.verdura.app.viewmodel.AuthViewModelFactory

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val db by lazy { AppDatabase.getInstance(this) }
    val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(FirebaseAuthRepository(), db.postDao(), db.userDao())
    }
    private var navController: NavController? = null

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
        binding.navHostFragment.isVisible = false
        binding.bottomNavigation.isVisible = false
        supportFragmentManager.beginTransaction()
            .replace(R.id.authContainer, LoginFragment())
            .commit()
    }

    private fun showMainContent() {
        binding.authContainer.isVisible = false
        binding.navHostFragment.isVisible = true
        binding.bottomNavigation.isVisible = true
        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as? NavHostFragment ?: return
        navController = navHostFragment.navController

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            val currentDestination = navController?.currentDestination?.id
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (currentDestination != R.id.homeFragment) {
                        navController?.navigate(R.id.homeFragment)
                    }
                    true
                }
                R.id.nav_explore -> {
                    if (currentDestination != R.id.plantExploreFragment) {
                        navController?.navigate(R.id.plantExploreFragment)
                    }
                    true
                }
                R.id.nav_profile -> {
                    if (currentDestination != R.id.profileFragment) {
                        navController?.navigate(R.id.profileFragment)
                    }
                    true
                }
                else -> false
            }
        }

        navController?.addOnDestinationChangedListener { _, destination, _ ->
            val selectedItemId = when (destination.id) {
                R.id.homeFragment, R.id.createPostFragment,
                R.id.postDetailFragment, R.id.editPostFragment -> R.id.nav_home
                R.id.plantExploreFragment -> R.id.nav_explore
                R.id.profileFragment, R.id.editProfileFragment,
                R.id.myPostsFragment -> R.id.nav_profile
                else -> null
            }
            selectedItemId?.let {
                if (binding.bottomNavigation.selectedItemId != it) {
                    binding.bottomNavigation.selectedItemId = it
                }
            }
        }
    }

    fun getNavController(): NavController? = navController
}
