package com.verdura.app.repository

import com.verdura.app.data.PostDao
import com.verdura.app.model.Post
import kotlinx.coroutines.flow.Flow
import kotlin.math.cos

class LocalPostRepository(
    private val postDao: PostDao
) : PostRepository {

    override fun getAllPosts(): Flow<List<Post>> {
        return postDao.getAllPosts()
    }

    override fun getPostsByUser(userId: String): Flow<List<Post>> {
        return postDao.getPostsByUser(userId)
    }

    override fun getPostById(postId: String): Flow<Post?> {
        return postDao.getPostById(postId)
    }

    override fun getPostsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Post>> {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(latitude)))

        return postDao.getPostsInBounds(
            minLat = latitude - latDelta,
            maxLat = latitude + latDelta,
            minLon = longitude - lonDelta,
            maxLon = longitude + lonDelta
        )
    }

    override suspend fun createPost(post: Post): Result<Post> {
        return try {
            postDao.insertPost(post)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: Post): Result<Post> {
        return try {
            postDao.updatePost(post)
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postDao.deletePost(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPosts(): Result<Unit> {
        return Result.success(Unit)
    }
}
