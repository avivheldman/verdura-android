package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var operationType: String = "",
    var postId: String = "",
    var postData: String? = null,
    var createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_CREATE = "CREATE"
        const val TYPE_UPDATE = "UPDATE"
        const val TYPE_DELETE = "DELETE"
    }
}
