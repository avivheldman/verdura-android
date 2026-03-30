package com.verdura.app.repository

import android.net.Uri
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun getAllPosts(): Flow<List<Post>>
    fun getPostsByUser(userId: String): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    suspend fun createPost(post: Post): Result<Post>
    suspend fun updatePost(post: Post): Result<Post>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String>
    suspend fun syncPosts(): Result<Unit>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>
    suspend fun addComment(postId: String, comment: Comment): Result<Unit>
}
