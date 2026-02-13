package com.verdura.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdura.app.model.User
import com.verdura.app.repository.AuthRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _loginResult = MutableLiveData<Result<User>?>()
    val loginResult: LiveData<Result<User>?> = _loginResult

    private val _registerResult = MutableLiveData<Result<User>?>()
    val registerResult: LiveData<Result<User>?> = _registerResult

    private val _resetPasswordResult = MutableLiveData<Result<Unit>?>()
    val resetPasswordResult: LiveData<Result<Unit>?> = _resetPasswordResult

    val isLoggedIn: Boolean
        get() = authRepository.isLoggedIn

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                _currentUser.value = user
                _authState.value = if (user != null) {
                    AuthState.Authenticated(user)
                } else {
                    AuthState.Unauthenticated
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, password)
            _loginResult.value = result
            if (result.isFailure) {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(email, password, displayName)
            _registerResult.value = result
            if (result.isFailure) {
                _authState.value = AuthState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            _resetPasswordResult.value = result
        }
    }

    fun clearLoginResult() {
        _loginResult.value = null
    }

    fun clearRegisterResult() {
        _registerResult.value = null
    }

    fun clearResetPasswordResult() {
        _resetPasswordResult.value = null
    }

    fun getCurrentUserId(): String? = authRepository.getCurrentUserId()
}

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}
