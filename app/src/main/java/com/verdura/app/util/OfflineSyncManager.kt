package com.verdura.app.util

import com.google.gson.Gson
import com.verdura.app.data.PendingOperationDao
import com.verdura.app.model.PendingOperation
import com.verdura.app.model.Post
import com.verdura.app.repository.FirebasePostRepository
import com.verdura.app.repository.NetworkChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OfflineSyncManager(
    private val pendingOperationDao: PendingOperationDao,
    private val firebaseRepository: FirebasePostRepository,
    private val networkChecker: NetworkChecker
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getPendingOperationsCount(): Flow<Int> {
        return pendingOperationDao.getPendingOperationsCount()
    }

    suspend fun queueOperation(type: String, postId: String, post: Post? = null) {
        val postData = post?.let { gson.toJson(it) }
        val operation = PendingOperation(
            operationType = type,
            postId = postId,
            postData = postData
        )
        pendingOperationDao.insert(operation)
    }

    suspend fun syncPendingOperations(): Result<Int> {
        if (!networkChecker.isNetworkAvailable()) {
            return Result.failure(Exception("No network connection"))
        }

        val pendingOps = pendingOperationDao.getAllPendingOperationsSync()
        var successCount = 0

        for (operation in pendingOps) {
            val result = when (operation.operationType) {
                PendingOperation.TYPE_CREATE -> {
                    operation.postData?.let { data ->
                        val post = gson.fromJson(data, Post::class.java)
                        firebaseRepository.createPost(post)
                    } ?: Result.failure(Exception("No post data"))
                }
                PendingOperation.TYPE_UPDATE -> {
                    operation.postData?.let { data ->
                        val post = gson.fromJson(data, Post::class.java)
                        firebaseRepository.updatePost(post)
                    } ?: Result.failure(Exception("No post data"))
                }
                PendingOperation.TYPE_DELETE -> {
                    firebaseRepository.deletePost(operation.postId)
                }
                else -> Result.failure(Exception("Unknown operation type"))
            }

            if (result.isSuccess) {
                pendingOperationDao.deleteById(operation.id)
                successCount++
            }
        }

        return Result.success(successCount)
    }

    fun startAutoSync() {
        scope.launch {
            if (networkChecker.isNetworkAvailable()) {
                syncPendingOperations()
            }
        }
    }
}
