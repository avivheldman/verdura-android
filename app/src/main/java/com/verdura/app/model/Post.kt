package com.verdura.app.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class Post(
    @PrimaryKey
    val id: String,
    val userId: String,
    val text: String,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long
)
