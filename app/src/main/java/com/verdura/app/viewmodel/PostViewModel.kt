package com.verdura.app.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import com.verdura.app.repository.PostRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class PostViewModel(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PostUiState())
    val uiState: StateFlow<PostUiState> = _uiState.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _selectedPost = MutableStateFlow<Post?>(null)
    val selectedPost: StateFlow<Post?> = _selectedPost.asStateFlow()

    init {
        loadAllPosts()
    }

    fun loadAllPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                postRepository.getAllPosts().collect { postList ->
                    _posts.value = postList
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadPostsByUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                postRepository.getPostsByUser(userId).collect { postList ->
                    _posts.value = postList
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadPostById(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                postRepository.getPostById(postId).collect { post ->
                    _selectedPost.value = post
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun createPost(
        userId: String,
        text: String,
        imageUri: Uri? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val postId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            var imageUrl: String? = null
            if (imageUri != null) {
                val uploadResult = postRepository.uploadPostImage(postId, imageUri)
                if (uploadResult.isFailure) {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to upload image") }
                    return@launch
                }
                imageUrl = uploadResult.getOrNull()
            }

            val post = Post(
                id = postId,
                userId = userId,
                text = text,
                imageUrl = imageUrl,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            val result = postRepository.createPost(post)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, postCreated = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun updatePost(post: Post) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val updatedPost = post.copy(updatedAt = System.currentTimeMillis())
            val result = postRepository.updatePost(updatedPost)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, postUpdated = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = postRepository.deletePost(postId)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, postDeleted = true) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    fun syncPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = postRepository.syncPosts()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    loadAllPosts()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            )
        }
    }

    suspend fun uploadImage(postId: String, imageUri: Uri): Result<String> {
        return postRepository.uploadPostImage(postId, imageUri)
    }

    fun toggleLike(postId: String, userId: String, isCurrentlyLiked: Boolean) {
        viewModelScope.launch {
            val result = if (isCurrentlyLiked) {
                postRepository.unlikePost(postId, userId)
            } else {
                postRepository.likePost(postId, userId)
            }
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addComment(postId: String, userId: String, userName: String, text: String) {
        viewModelScope.launch {
            val comment = Comment(
                userId = userId,
                userName = userName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            val result = postRepository.addComment(postId, comment)
            result.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetPostCreated() {
        _uiState.update { it.copy(postCreated = false) }
    }

    fun resetPostUpdated() {
        _uiState.update { it.copy(postUpdated = false) }
    }

    fun resetPostDeleted() {
        _uiState.update { it.copy(postDeleted = false) }
    }

    class Factory(private val postRepository: PostRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PostViewModel::class.java)) {
                return PostViewModel(postRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class PostUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val postCreated: Boolean = false,
    val postUpdated: Boolean = false,
    val postDeleted: Boolean = false
)
