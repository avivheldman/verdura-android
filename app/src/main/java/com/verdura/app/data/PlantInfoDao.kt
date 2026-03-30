package com.verdura.app.data

import androidx.room.*
import com.verdura.app.model.PlantInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantInfoDao {
    @Query("SELECT * FROM plant_info ORDER BY commonName ASC")
    fun getAllPlants(): Flow<List<PlantInfo>>

    @Query("""SELECT * FROM plant_info 
        WHERE commonName LIKE :query 
           OR commonName LIKE :query || '%' 
           OR commonName LIKE '% ' || :query || '%'
           OR scientificName LIKE :query || '%' 
           OR scientificName LIKE '% ' || :query || '%'
        ORDER BY 
            CASE WHEN commonName LIKE :query THEN 0 
                 WHEN commonName LIKE :query || '%' THEN 1 
                 WHEN commonName LIKE '% ' || :query || '%' THEN 2 
                 ELSE 3 END, 
            commonName ASC""")
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

    @Query("SELECT MAX(cachedAt) FROM plant_info")
    suspend fun getLatestCacheTimestamp(): Long?

    @Query("SELECT * FROM plant_info ORDER BY commonName ASC LIMIT :limit OFFSET :offset")
    suspend fun getPlantsPaged(limit: Int, offset: Int): List<PlantInfo>

    @Query("SELECT * FROM plant_info ORDER BY commonName ASC LIMIT :limit")
    suspend fun getAllPlantsOnce(limit: Int = 60): List<PlantInfo>

    @Query("""SELECT * FROM plant_info 
        WHERE commonName LIKE :query 
           OR commonName LIKE :query || '%' 
           OR commonName LIKE '% ' || :query || '%'
           OR scientificName LIKE :query || '%' 
           OR scientificName LIKE '% ' || :query || '%'
        ORDER BY 
            CASE WHEN commonName LIKE :query THEN 0 
                 WHEN commonName LIKE :query || '%' THEN 1 
                 WHEN commonName LIKE '% ' || :query || '%' THEN 2 
                 ELSE 3 END, 
            commonName ASC 
        LIMIT :limit""")
    suspend fun searchPlantsOnce(query: String, limit: Int = 60): List<PlantInfo>

    @Query("""UPDATE plant_info SET 
        cycle = COALESCE(:cycle, cycle), 
        watering = COALESCE(:watering, watering), 
        sunlight = COALESCE(:sunlight, sunlight) 
        WHERE id = :plantId""")
    suspend fun updateDetailFields(plantId: Int, cycle: String?, watering: String?, sunlight: String?)

    @Query("SELECT id FROM plant_info WHERE (cycle IS NULL OR cycle = '') AND (watering IS NULL OR watering = '') AND (sunlight IS NULL OR sunlight = '') LIMIT :limit")
    suspend fun getPlantIdsMissingData(limit: Int = 10): List<Int>

    @Query("""SELECT * FROM plant_info 
        WHERE scientificName IS NOT NULL AND scientificName != ''
          AND ((cycle IS NULL OR cycle = '') AND (watering IS NULL OR watering = '') AND (sunlight IS NULL OR sunlight = ''))
        LIMIT :limit""")
    suspend fun getPlantsMissingCareData(limit: Int = 20): List<PlantInfo>
}
