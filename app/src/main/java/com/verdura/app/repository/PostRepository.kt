package com.verdura.app.repository

import com.verdura.app.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(): Flow<List<Post>>
    fun getPostsByUser(userId: String): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    fun getPostsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Post>>
    suspend fun createPost(post: Post): Result<Post>
    suspend fun updatePost(post: Post): Result<Post>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun syncPosts(): Result<Unit>
}
