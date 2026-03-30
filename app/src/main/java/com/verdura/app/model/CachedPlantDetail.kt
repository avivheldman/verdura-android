package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_plant_details")
data class CachedPlantDetail(
    @PrimaryKey
    val plantId: Int,
    val detailJson: String,
    val cachedAt: Long = System.currentTimeMillis()
)
