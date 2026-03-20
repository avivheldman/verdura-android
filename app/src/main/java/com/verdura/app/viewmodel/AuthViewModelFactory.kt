package com.verdura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.verdura.app.data.PostDao
import com.verdura.app.data.UserDao
import com.verdura.app.repository.AuthRepository

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val postDao: PostDao? = null,
    private val userDao: UserDao? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, postDao, userDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
