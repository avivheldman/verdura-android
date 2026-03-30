package com.verdura.app.repository

import com.verdura.app.data.PostDao
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

class CombinedPostRepository(
    private val localRepository: LocalPostRepository,
    private val firebaseRepository: FirebasePostRepository,
    private val postDao: PostDao,
    private val networkChecker: NetworkChecker
) : PostRepository {

    override fun getAllPosts(): Flow<List<Post>> {
        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.getAllPosts().onEach { posts ->
                postDao.insertPosts(posts)
            }
        } else {
            localRepository.getAllPosts()
        }
    }

    override fun getPostsByUser(userId: String): Flow<List<Post>> {
        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.getPostsByUser(userId).onEach { posts ->
                postDao.insertPosts(posts)
            }
        } else {
            localRepository.getPostsByUser(userId)
        }
    }

    override fun getPostById(postId: String): Flow<Post?> {
        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.getPostById(postId).onEach { post ->
                post?.let { postDao.insertPost(it) }
            }
        } else {
            localRepository.getPostById(postId)
        }
    }

    override suspend fun createPost(post: Post): Result<Post> {
        localRepository.createPost(post)

        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.createPost(post)
        } else {
            Result.success(post)
        }
    }

    override suspend fun updatePost(post: Post): Result<Post> {
        localRepository.updatePost(post)

        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.updatePost(post)
        } else {
            Result.success(post)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        localRepository.deletePost(postId)

        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.deletePost(postId)
        } else {
            Result.success(Unit)
        }
    }

    override suspend fun uploadPostImage(postId: String, imageUri: android.net.Uri): Result<String> {
        return firebaseRepository.uploadPostImage(postId, imageUri)
    }

    override suspend fun syncPosts(): Result<Unit> {
        return try {
            if (networkChecker.isNetworkAvailable()) {
                val remotePosts = firebaseRepository.getAllPosts().first()
                postDao.deleteAllPosts()
                postDao.insertPosts(remotePosts)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return firebaseRepository.likePost(postId, userId)
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return firebaseRepository.unlikePost(postId, userId)
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return firebaseRepository.addComment(postId, comment)
    }
}

interface NetworkChecker {
    fun isNetworkAvailable(): Boolean
}
