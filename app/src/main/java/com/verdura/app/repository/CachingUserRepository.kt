package com.verdura.app.repository

import android.net.Uri
import com.verdura.app.data.UserDao
import com.verdura.app.model.User

class CachingUserRepository(
    private val remoteRepository: FirebaseUserRepository,
    private val userDao: UserDao
) : UserRepository {

    override suspend fun getUser(userId: String): Result<User> {
        val cached = userDao.getById(userId)
        if (cached != null) {
            refreshFromRemote(userId)
            return Result.success(cached)
        }

        val result = remoteRepository.getUser(userId)
        result.onSuccess { user -> userDao.insert(user) }
        return result
    }

    override suspend fun updateUser(user: User): Result<User> {
        val result = remoteRepository.updateUser(user)
        result.onSuccess { updated -> userDao.insert(updated) }
        return result
    }

    override suspend fun updateProfilePhoto(userId: String, photoUri: Uri): Result<String> {
        val result = remoteRepository.updateProfilePhoto(userId, photoUri)
        result.onSuccess { downloadUrl ->
            userDao.getById(userId)?.let { existing ->
                userDao.insert(existing.copy(photoUrl = downloadUrl))
            }
        }
        return result
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        val result = remoteRepository.deleteUser(userId)
        result.onSuccess { userDao.deleteAll() }
        return result
    }

    private suspend fun refreshFromRemote(userId: String) {
        try {
            val result = remoteRepository.getUser(userId)
            result.onSuccess { user -> userDao.insert(user) }
        } catch (_: Exception) {
            // Silently fail -- cached data is already returned
        }
    }
}
