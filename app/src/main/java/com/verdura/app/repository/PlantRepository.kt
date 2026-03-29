package com.verdura.app.repository

import android.util.LruCache
import com.verdura.app.api.PlantApiService
import com.verdura.app.api.RetrofitClient
import com.verdura.app.api.TrefleApiService
import com.verdura.app.api.models.PlantDetailResponse
import com.verdura.app.api.models.TrefleSpeciesDetail
import com.verdura.app.data.PlantInfoDao
import com.verdura.app.model.PlantInfo
import com.verdura.app.util.ApiConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PlantRepository(
    private val plantInfoDao: PlantInfoDao,
    private val apiService: PlantApiService = RetrofitClient.plantApiService,
    private val trefleService: TrefleApiService = RetrofitClient.trefleApiService
) {
    companion object {
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L
        private const val DETAIL_CACHE_SIZE = 50
        private val perenualCache = LruCache<Int, PlantDetailResponse>(DETAIL_CACHE_SIZE)
        private val trefleCache = LruCache<String, Result<TrefleSpeciesDetail>>(DETAIL_CACHE_SIZE)
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
            val response = apiService.getPlantList(ApiConfig.getPlantApiKey(), page, query)
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

    suspend fun fetchPlantDetails(plantId: Int): Result<PlantDetailResponse> {
        perenualCache.get(plantId)?.let { return Result.success(it) }

        return try {
            val response = apiService.getPlantDetails(plantId, ApiConfig.getPlantApiKey())
            perenualCache.put(plantId, response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchTrefleDetails(scientificName: String?, commonName: String): Result<TrefleSpeciesDetail> {
        val cacheKey = scientificName ?: commonName
        trefleCache.get(cacheKey)?.let { return it }

        val result = try {
            val token = ApiConfig.getTrefleToken()
            val genus = scientificName?.split(" ")?.firstOrNull()

            val searchQuery = genus ?: commonName
            val searchResponse = trefleService.searchSpecies(token, searchQuery)
            val firstMatch = searchResponse.data.firstOrNull()
                ?: return Result.failure<TrefleSpeciesDetail>(Exception("No match found on Trefle"))
                    .also { trefleCache.put(cacheKey, it) }
            val detailResponse = trefleService.getSpeciesDetails(firstMatch.id, token)
            Result.success(detailResponse.data)
        } catch (e: Exception) {
            Result.failure(e)
        }

        trefleCache.put(cacheKey, result)
        return result
    }

    fun getCachedPlants(): Flow<List<PlantInfo>> {
        return plantInfoDao.getAllPlants()
    }

    suspend fun isCacheValid(): Boolean {
        val count = plantInfoDao.getCount()
        return count > 0
    }
}
