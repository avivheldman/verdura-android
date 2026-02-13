package com.verdura.app.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.verdura.app.model.User
import kotlinx.coroutines.tasks.await

interface UserRepository {
    suspend fun getUser(userId: String): Result<User>
    suspend fun updateUser(user: User): Result<User>
    suspend fun updateProfilePhoto(userId: String, photoUri: Uri): Result<String>
    suspend fun deleteUser(userId: String): Result<Unit>
}

class FirebaseUserRepository : UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val usersCollection = firestore.collection("users")

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = usersCollection.document(userId).get().await()
            if (document.exists()) {
                val user = User(
                    id = document.id,
                    email = document.getString("email") ?: "",
                    displayName = document.getString("displayName"),
                    photoUrl = document.getString("photoUrl"),
                    createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                )
                Result.success(user)
            } else {
                auth.currentUser?.let { firebaseUser ->
                    val user = User(
                        id = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        displayName = firebaseUser.displayName,
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = firebaseUser.metadata?.creationTimestamp ?: System.currentTimeMillis()
                    )
                    Result.success(user)
                } ?: Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return try {
            val userData = mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "photoUrl" to user.photoUrl,
                "createdAt" to user.createdAt
            )
            usersCollection.document(user.id).set(userData).await()
            auth.currentUser?.let { firebaseUser ->
                val profileUpdates = userProfileChangeRequest {
                    displayName = user.displayName
                    user.photoUrl?.let { photoUri = Uri.parse(it) }
                }
                firebaseUser.updateProfile(profileUpdates).await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProfilePhoto(userId: String, photoUri: Uri): Result<String> {
        return try {
            val photoRef = storage.reference.child("profile_photos/$userId.jpg")
            photoRef.putFile(photoUri).await()
            val downloadUrl = photoRef.downloadUrl.await().toString()
            usersCollection.document(userId).update("photoUrl", downloadUrl).await()
            auth.currentUser?.let { firebaseUser ->
                val profileUpdates = userProfileChangeRequest { this.photoUri = Uri.parse(downloadUrl) }
                firebaseUser.updateProfile(profileUpdates).await()
            }
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            usersCollection.document(userId).delete().await()
            storage.reference.child("profile_photos/$userId.jpg").delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
