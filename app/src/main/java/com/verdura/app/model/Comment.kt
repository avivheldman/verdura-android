package com.verdura.app.model

data class Comment(
    var userId: String = "",
    var userName: String = "",
    var text: String = "",
    var timestamp: Long = 0L
)
