package com.verdura.app.repository

import com.google.gson.Gson
import com.verdura.app.api.RetrofitClient
import com.verdura.app.api.TrefleApiService
import com.verdura.app.api.models.TrefleSpeciesDetail
import com.verdura.app.data.PlantInfoDao
import com.verdura.app.data.TrefleDetailCacheDao
import com.verdura.app.model.CachedTrefleDetail
import com.verdura.app.model.PlantInfo
import com.verdura.app.util.ApiConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

data class PaginatedPlants(
    val plants: List<PlantInfo>,
    val lastPage: Int
)

class PlantRepository(
    private val plantInfoDao: PlantInfoDao,
    private val trefleCacheDao: TrefleDetailCacheDao? = null,
    private val trefleService: TrefleApiService = RetrofitClient.trefleApiService
) {
    companion object {
        private const val PAGE_SIZE = 30
        private const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L
        private val gson = Gson()

        private val INDOOR_GENERA = listOf(
            "aglaonema", "alocasia", "aloe", "anthurium", "aspidistra",
            "asplenium", "begonia", "caladium", "calathea", "chlorophytum",
            "clivia", "codiaeum", "crassula", "cyclamen", "dieffenbachia",
            "dracaena", "echeveria", "epipremnum", "ficus", "fittonia",
            "gardenia", "haworthia", "hedera", "hoya", "kalanchoe",
            "maranta", "monstera", "nephrolepis", "peperomia", "philodendron",
            "pilea", "sansevieria", "schefflera", "sedum", "spathiphyllum",
            "tradescantia", "yucca", "zamioculcas", "saintpaulia", "streptocarpus"
        )

        val INDOOR_GENERA_FILTER = INDOOR_GENERA.joinToString(",")

        fun lightToLabel(light: Int): String = when {
            light <= 3 -> "Low light"
            light <= 5 -> "Part shade"
            light <= 7 -> "Bright indirect"
            else -> "Full sun"
        }

        fun humidityToLabel(humidity: Int): String = when {
            humidity <= 2 -> "Minimum"
            humidity <= 4 -> "Average"
            humidity <= 6 -> "Frequent"
            else -> "Abundant"
        }

        val genusCareDefaults = mapOf(
            "aglaonema" to Triple("Perennial", "Average", "Low light"),
            "alocasia" to Triple("Perennial", "Frequent", "Bright indirect"),
            "aloe" to Triple("Perennial", "Minimum", "Bright indirect"),
            "anthurium" to Triple("Perennial", "Frequent", "Bright indirect"),
            "aspidistra" to Triple("Perennial", "Average", "Low light"),
            "asplenium" to Triple("Perennial", "Frequent", "Low light"),
            "begonia" to Triple("Perennial", "Average", "Bright indirect"),
            "caladium" to Triple("Perennial", "Frequent", "Bright indirect"),
            "calathea" to Triple("Perennial", "Frequent", "Low light"),
            "chlorophytum" to Triple("Perennial", "Average", "Bright indirect"),
            "clivia" to Triple("Perennial", "Average", "Bright indirect"),
            "codiaeum" to Triple("Perennial", "Average", "Bright indirect"),
            "crassula" to Triple("Perennial", "Minimum", "Bright indirect"),
            "cyclamen" to Triple("Perennial", "Average", "Bright indirect"),
            "dieffenbachia" to Triple("Perennial", "Average", "Bright indirect"),
            "dracaena" to Triple("Perennial", "Minimum", "Bright indirect"),
            "echeveria" to Triple("Perennial", "Minimum", "Full sun"),
            "epipremnum" to Triple("Perennial", "Average", "Low light"),
            "ficus" to Triple("Perennial", "Average", "Bright indirect"),
            "fittonia" to Triple("Perennial", "Frequent", "Low light"),
            "gardenia" to Triple("Perennial", "Average", "Bright indirect"),
            "haworthia" to Triple("Perennial", "Minimum", "Bright indirect"),
            "hedera" to Triple("Perennial", "Average", "Part shade"),
            "hoya" to Triple("Perennial", "Average", "Bright indirect"),
            "kalanchoe" to Triple("Perennial", "Minimum", "Bright indirect"),
            "maranta" to Triple("Perennial", "Frequent", "Low light"),
            "monstera" to Triple("Perennial", "Average", "Bright indirect"),
            "nephrolepis" to Triple("Perennial", "Frequent", "Part shade"),
            "peperomia" to Triple("Perennial", "Average", "Bright indirect"),
            "philodendron" to Triple("Perennial", "Average", "Bright indirect"),
            "pilea" to Triple("Perennial", "Average", "Bright indirect"),
            "sansevieria" to Triple("Perennial", "Minimum", "Low light"),
            "schefflera" to Triple("Perennial", "Average", "Bright indirect"),
            "sedum" to Triple("Perennial", "Minimum", "Full sun"),
            "spathiphyllum" to Triple("Perennial", "Average", "Low light"),
            "tradescantia" to Triple("Perennial", "Average", "Bright indirect"),
            "yucca" to Triple("Perennial", "Minimum", "Full sun"),
            "zamioculcas" to Triple("Perennial", "Minimum", "Low light"),
            "saintpaulia" to Triple("Perennial", "Average", "Bright indirect"),
            "streptocarpus" to Triple("Perennial", "Average", "Bright indirect"),
        )
    }

    suspend fun fetchPlantsWithCache(
        query: String? = null,
        page: Int = 1
    ): Result<PaginatedPlants> {
        if (!query.isNullOrBlank()) {
            val localResults = plantInfoDao.searchPlantsOnce(query, PAGE_SIZE)
            if (localResults.isNotEmpty()) {
                return Result.success(PaginatedPlants(localResults, 1))
            }
            return searchTrefleApi(query)
        }

        if (page == 1) {
            val latestTimestamp = plantInfoDao.getLatestCacheTimestamp()
            if (latestTimestamp != null &&
                System.currentTimeMillis() - latestTimestamp < CACHE_DURATION_MS
            ) {
                val count = plantInfoDao.getCount()
                if (count > 0) {
                    val cached = plantInfoDao.getPlantsPaged(PAGE_SIZE, 0)
                    val estimatedPages = (count + PAGE_SIZE - 1) / PAGE_SIZE
                    return Result.success(PaginatedPlants(cached, estimatedPages))
                }
            }
        }

        val apiResult = fetchPlantsFromApi(null, page)
        if (apiResult.isSuccess) return apiResult

        val stale = plantInfoDao.getAllPlantsOnce(PAGE_SIZE)
        if (stale.isNotEmpty()) {
            return Result.success(PaginatedPlants(stale, 1))
        }
        return apiResult
    }

    private suspend fun searchTrefleApi(query: String): Result<PaginatedPlants> {
        return try {
            val token = ApiConfig.getTrefleToken()
            val response = trefleService.searchSpecies(token, query)
            val plants = response.data
                .filter { !it.commonName.isNullOrBlank() }
                .map { mapToPlantInfo(it) }
            if (plants.isNotEmpty()) {
                plantInfoDao.insertAll(plants)
            }
            Result.success(PaginatedPlants(plants, 1))
        } catch (e: Exception) {
            val localResults = plantInfoDao.searchPlantsOnce(query, PAGE_SIZE)
            if (localResults.isNotEmpty()) {
                Result.success(PaginatedPlants(localResults, 1))
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun fetchPlantsFromApi(
        query: String? = null,
        page: Int = 1
    ): Result<PaginatedPlants> {
        return try {
            val token = ApiConfig.getTrefleToken()
            val response = if (!query.isNullOrBlank()) {
                trefleService.searchSpecies(token, query)
            } else {
                trefleService.listSpecies(token, page, INDOOR_GENERA_FILTER)
            }
            val plants = response.data
                .filter { !it.commonName.isNullOrBlank() }
                .map { mapToPlantInfo(it) }
            if (plants.isNotEmpty()) {
                plantInfoDao.insertAll(plants)
            }
            val total = response.meta?.total ?: plants.size
            val totalPages = if (total > 0) (total + PAGE_SIZE - 1) / PAGE_SIZE else 1
            Result.success(PaginatedPlants(plants, totalPages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToPlantInfo(summary: com.verdura.app.api.models.TrefleSpeciesSummary): PlantInfo {
        val genus = summary.genus?.lowercase()
        val defaults = genus?.let { genusCareDefaults[it] }
        return PlantInfo(
            id = summary.id,
            commonName = summary.commonName ?: summary.scientificName,
            scientificName = summary.scientificName,
            cycle = defaults?.first,
            watering = defaults?.second,
            sunlight = defaults?.third,
            imageUrl = summary.imageUrl
        )
    }

    suspend fun fetchPlantDetails(
        speciesId: Int,
        scientificName: String? = null
    ): Result<TrefleSpeciesDetail> {
        val cacheKey = scientificName?.split(",")?.first()?.trim()

        if (!cacheKey.isNullOrBlank()) {
            try {
                trefleCacheDao?.get(cacheKey)?.let { cached ->
                    return Result.success(
                        gson.fromJson(cached.detailJson, TrefleSpeciesDetail::class.java)
                    )
                }
            } catch (_: Exception) { }
        }

        return try {
            val token = ApiConfig.getTrefleToken()
            val detail = trefleService.getSpeciesDetails(speciesId, token).data

            val key = cacheKey ?: detail.scientificName ?: speciesId.toString()
            try {
                trefleCacheDao?.insert(CachedTrefleDetail(key, gson.toJson(detail)))
            } catch (_: Exception) { }

            backfillPlantInfoFromTrefle(speciesId, detail, scientificName)
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun backfillPlantInfoFromTrefle(
        plantId: Int,
        detail: TrefleSpeciesDetail,
        scientificName: String? = null
    ) {
        try {
            var sunlight = detail.growth?.light?.let { lightToLabel(it) }
            var watering = detail.growth?.let {
                (it.soilHumidity ?: it.atmosphericHumidity)?.let { h -> humidityToLabel(h) }
            }
            var cycle = detail.duration?.firstOrNull()?.replaceFirstChar { it.uppercase() }

            if (sunlight == null && watering == null && cycle == null) {
                val genus = (scientificName ?: detail.scientificName)
                    ?.split(" ")?.firstOrNull()?.lowercase()
                genusCareDefaults[genus]?.let { (c, w, s) ->
                    cycle = c; watering = w; sunlight = s
                }
            }

            if (sunlight != null || watering != null || cycle != null) {
                plantInfoDao.updateDetailFields(plantId, cycle, watering, sunlight)
            }
        } catch (_: Exception) { }
    }

    private fun genusFallback(scientificName: String?): Triple<String, String, String>? {
        val genus = scientificName?.split(" ")?.firstOrNull()?.lowercase() ?: return null
        return genusCareDefaults[genus]
    }

    suspend fun backfillFromGenusFallback(): Int {
        val missing = plantInfoDao.getPlantsMissingCareData(60)
        var count = 0
        for (plant in missing) {
            genusFallback(plant.scientificName)?.let { (cycle, watering, sunlight) ->
                plantInfoDao.updateDetailFields(plant.id, cycle, watering, sunlight)
                count++
            }
        }
        return count
    }

    private fun lightToLabel(light: Int): String = Companion.lightToLabel(light)

    private fun humidityToLabel(humidity: Int): String = Companion.humidityToLabel(humidity)

    suspend fun backfillFromCachedDetails(): Int {
        val missing = plantInfoDao.getPlantsMissingCareData(60)
        if (missing.isEmpty()) return 0
        var enriched = 0
        for (plant in missing) {
            val sciName = plant.scientificName?.split(",")?.first()?.trim()
            if (!sciName.isNullOrBlank()) {
                try {
                    trefleCacheDao?.get(sciName)?.let { cached ->
                        val detail = gson.fromJson(cached.detailJson, TrefleSpeciesDetail::class.java)
                        backfillPlantInfoFromTrefle(plant.id, detail, plant.scientificName)
                        enriched++
                    }
                } catch (_: Exception) { }
            }
        }
        return enriched
    }

    suspend fun prefetchCareFromTrefle(
        limit: Int = 60,
        batchSize: Int = 5,
        onBatchReady: suspend () -> Unit = {}
    ) {
        val plants = plantInfoDao.getPlantsMissingCareData(limit)
        var batchCount = 0
        for (plant in plants) {
            try {
                val result = withTimeoutOrNull(15_000L) {
                    fetchPlantDetails(plant.id, plant.scientificName)
                } ?: continue
                result.onSuccess {
                    batchCount++
                    if (batchCount % batchSize == 0) onBatchReady()
                }
            } catch (_: Exception) { }
            delay(200)
        }
        if (batchCount % batchSize != 0) onBatchReady()
    }

    suspend fun getCachedPlantsOnce(query: String? = null): List<PlantInfo> {
        return if (query.isNullOrBlank()) {
            plantInfoDao.getAllPlantsOnce()
        } else {
            plantInfoDao.searchPlantsOnce(query)
        }
    }

}
