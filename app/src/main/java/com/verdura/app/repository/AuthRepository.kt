package com.verdura.app.repository

import com.verdura.app.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isLoggedIn: Boolean

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String): Result<User>
    suspend fun logout()
    suspend fun resetPassword(email: String): Result<Unit>
    fun getCurrentUserId(): String?
}
