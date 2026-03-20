package com.verdura.app.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdura.app.model.User
import com.verdura.app.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _updateSuccess = MutableLiveData<Boolean>()
    val updateSuccess: LiveData<Boolean> = _updateSuccess

    fun loadUser(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.getUser(userId)
            result.fold(
                onSuccess = { _user.value = it },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun updateUser(user: User) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.updateUser(user)
            result.fold(
                onSuccess = {
                    _user.value = it
                    _updateSuccess.value = true
                },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun updateProfilePhoto(userId: String, photoUri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.updateProfilePhoto(userId, photoUri)
            result.fold(
                onSuccess = { downloadUrl ->
                    _user.value = _user.value?.copy(photoUrl = downloadUrl)
                    _updateSuccess.value = true
                },
                onFailure = { _error.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearUpdateSuccess() {
        _updateSuccess.value = false
    }
}
