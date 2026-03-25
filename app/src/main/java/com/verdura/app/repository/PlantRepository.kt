package com.verdura.app.repository

import com.verdura.app.api.PlantApiService
import com.verdura.app.api.RetrofitClient
import com.verdura.app.data.PlantInfoDao
import com.verdura.app.model.PlantInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlantRepository(
    private val plantInfoDao: PlantInfoDao,
    private val apiService: PlantApiService = RetrofitClient.plantApiService
) {
    companion object {
        const val API_KEY = "sk-XXXX-placeholder"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    fun searchPlants(query: String): Flow<List<PlantInfo>> = flow {
        val cached = plantInfoDao.searchPlants(query)
        cached.collect { localResults ->
            if (localResults.isNotEmpty()) {
                emit(localResults)
            }
        }
    }

    suspend fun fetchPlantsFromApi(query: String? = null, page: Int = 1): Result<List<PlantInfo>> {
        return try {
            val response = apiService.getPlantList(API_KEY, page, query)
            val plants = response.data.map { summary ->
                PlantInfo(
                    id = summary.id,
                    commonName = summary.commonName,
                    scientificName = summary.scientificName?.joinToString(", "),
                    cycle = summary.cycle,
                    watering = summary.watering,
                    sunlight = summary.sunlight?.joinToString(", "),
                    imageUrl = summary.defaultImage?.regularUrl
                        ?: summary.defaultImage?.mediumUrl
                        ?: summary.defaultImage?.smallUrl
                )
            }
            plantInfoDao.insertAll(plants)
            Result.success(plants)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedPlants(): Flow<List<PlantInfo>> {
        return plantInfoDao.getAllPlants()
    }

    suspend fun isCacheValid(): Boolean {
        val count = plantInfoDao.getCount()
        return count > 0
    }
}
