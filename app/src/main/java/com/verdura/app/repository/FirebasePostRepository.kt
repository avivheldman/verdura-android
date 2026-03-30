package com.verdura.app.repository

import android.net.Uri
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.verdura.app.model.Comment
import com.verdura.app.model.Post
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebasePostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) : PostRepository {

    private val postsCollection = firestore.collection("posts")

    override fun getAllPosts(): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getPostsByUser(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = postsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                } ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    override fun getPostById(postId: String): Flow<Post?> = callbackFlow {
        val listener = postsCollection.document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val post = snapshot?.toObject(Post::class.java)
                trySend(post)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createPost(post: Post): Result<Post> {
        return try {
            postsCollection.document(post.id).set(post).await()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: Post): Result<Post> {
        return try {
            postsCollection.document(post.id).set(post).await()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            try {
                storage.reference.child("post_images/$postId.jpg").delete().await()
            } catch (_: Exception) {
                // Image may not exist, ignore
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun uploadPostImage(postId: String, imageUri: Uri): Result<String> {
        return try {
            val imageRef = storage.reference.child("post_images/$postId.jpg")
            imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPosts(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            postsCollection.document(postId)
                .update("likedBy", FieldValue.arrayUnion(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            postsCollection.document(postId)
                .update("likedBy", FieldValue.arrayRemove(userId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            val commentMap = mapOf(
                "userId" to comment.userId,
                "userName" to comment.userName,
                "text" to comment.text,
                "timestamp" to comment.timestamp
            )
            postsCollection.document(postId)
                .update("comments", FieldValue.arrayUnion(commentMap))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
