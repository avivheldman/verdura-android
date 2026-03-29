package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    var id: String = "",
    var email: String = "",
    var displayName: String? = null,
    var photoUrl: String? = null,
    var createdAt: Long = 0L
)
