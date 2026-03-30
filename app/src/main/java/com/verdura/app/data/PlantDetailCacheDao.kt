package com.verdura.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.verdura.app.model.CachedPlantDetail

@Dao
interface PlantDetailCacheDao {
    @Query("SELECT * FROM cached_plant_details WHERE plantId = :plantId AND cachedAt > :minTimestamp LIMIT 1")
    suspend fun get(plantId: Int, minTimestamp: Long): CachedPlantDetail?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: CachedPlantDetail)
}
