package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_info")
data class PlantInfo(
    @PrimaryKey
    val id: Int,
    val commonName: String?,
    val scientificName: String?,
    val cycle: String?,
    val watering: String?,
    val sunlight: String?,
    val imageUrl: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
