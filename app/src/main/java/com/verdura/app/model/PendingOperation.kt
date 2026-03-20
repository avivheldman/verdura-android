package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationType: String,
    val postId: String,
    val postData: String?,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CREATE = "CREATE"
        const val TYPE_UPDATE = "UPDATE"
        const val TYPE_DELETE = "DELETE"
    }
}
