package com.verdura.app.repository

import com.verdura.app.data.PostDao
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import kotlinx.coroutines.flow.Flow

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

    override suspend fun uploadPostImage(postId: String, imageUri: android.net.Uri): Result<String> {
        return Result.failure(Exception("Local repository cannot upload images"))
    }

    override suspend fun syncPosts(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return Result.failure(Exception("Local repository does not support likes"))
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return Result.failure(Exception("Local repository does not support likes"))
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return Result.failure(Exception("Local repository does not support comments"))
    }
}
