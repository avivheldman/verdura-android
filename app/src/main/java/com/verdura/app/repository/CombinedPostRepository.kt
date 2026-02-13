package com.verdura.app.repository

import com.verdura.app.data.PostDao
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

    override fun getPostsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Post>> {
        return if (networkChecker.isNetworkAvailable()) {
            firebaseRepository.getPostsNearLocation(latitude, longitude, radiusKm).onEach { posts ->
                postDao.insertPosts(posts)
            }
        } else {
            localRepository.getPostsNearLocation(latitude, longitude, radiusKm)
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
}

interface NetworkChecker {
    fun isNetworkAvailable(): Boolean
}
