package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_trefle_details")
data class CachedTrefleDetail(
    @PrimaryKey
    val scientificName: String,
    val detailJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)
