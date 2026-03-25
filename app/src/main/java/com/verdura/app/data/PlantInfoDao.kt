package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.PlantInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantInfoDao {
    @Query("SELECT * FROM plant_info ORDER BY commonName ASC")
    fun getAllPlants(): Flow<List<PlantInfo>>

    @Query("SELECT * FROM plant_info WHERE commonName LIKE '%' || :query || '%' OR scientificName LIKE '%' || :query || '%'")
    fun searchPlants(query: String): Flow<List<PlantInfo>>

    @Query("SELECT * FROM plant_info WHERE id = :plantId")
    suspend fun getPlantById(plantId: Int): PlantInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(plants: List<PlantInfo>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plant: PlantInfo)

    @Query("DELETE FROM plant_info")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM plant_info")
    suspend fun getCount(): Int
}
