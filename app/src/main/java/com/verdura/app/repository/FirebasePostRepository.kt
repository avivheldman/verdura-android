package com.verdura.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.verdura.app.model.Post
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.cos

class FirebasePostRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
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

    override fun getPostsNearLocation(latitude: Double, longitude: Double, radiusKm: Double): Flow<List<Post>> = callbackFlow {
        val latDelta = radiusKm / 111.0
        val lonDelta = radiusKm / (111.0 * cos(Math.toRadians(latitude)))

        val listener = postsCollection
            .whereGreaterThanOrEqualTo("latitude", latitude - latDelta)
            .whereLessThanOrEqualTo("latitude", latitude + latDelta)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)
                }?.filter { post ->
                    post.longitude != null &&
                    post.longitude >= longitude - lonDelta &&
                    post.longitude <= longitude + lonDelta
                } ?: emptyList()
                trySend(posts)
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPosts(): Result<Unit> {
        return Result.success(Unit)
    }
}
