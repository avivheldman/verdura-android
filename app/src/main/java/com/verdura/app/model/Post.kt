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
    var id: String = "",
    var userId: String = "",
    var text: String = "",
    var imageUrl: String? = null,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var createdAt: Long = 0L,
    var updatedAt: Long = 0L,
    @Ignore
    var authorName: String? = null,
    @Ignore
    var likedBy: List<String> = emptyList(),
    @Ignore
    var comments: List<Comment> = emptyList()
) {
    constructor(
        id: String, userId: String, text: String, imageUrl: String?,
        latitude: Double?, longitude: Double?, createdAt: Long, updatedAt: Long
    ) : this(id, userId, text, imageUrl, latitude, longitude, createdAt, updatedAt, null, emptyList(), emptyList())
}
