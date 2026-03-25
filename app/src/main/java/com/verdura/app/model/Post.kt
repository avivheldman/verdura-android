package com.verdura.app.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
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
    val id: String = "",
    val userId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    @Ignore
    val authorName: String? = null
) {
    constructor(
        id: String, userId: String, text: String, imageUrl: String?,
        latitude: Double?, longitude: Double?, createdAt: Long, updatedAt: Long
    ) : this(id, userId, text, imageUrl, latitude, longitude, createdAt, updatedAt, null)
}
