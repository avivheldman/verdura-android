package com.verdura.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_info")
data class PlantInfo(
    @PrimaryKey
    var id: Int = 0,
    var commonName: String? = null,
    var scientificName: String? = null,
    var cycle: String? = null,
    var watering: String? = null,
    var sunlight: String? = null,
    var imageUrl: String? = null,
    var cachedAt: Long = System.currentTimeMillis()
)
