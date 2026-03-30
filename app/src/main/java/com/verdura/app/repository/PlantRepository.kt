package com.verdura.app.repository

import android.util.Log
import android.util.LruCache
import com.google.gson.Gson
import com.verdura.app.api.PlantApiService
import com.verdura.app.api.RetrofitClient
import com.verdura.app.api.TrefleApiService
import com.verdura.app.api.models.PlantDetailResponse
import com.verdura.app.api.models.TrefleSpeciesDetail
import com.verdura.app.data.PlantDetailCacheDao
import com.verdura.app.data.PlantInfoDao
import com.verdura.app.data.TrefleDetailCacheDao
import com.verdura.app.model.CachedPlantDetail
import com.verdura.app.model.CachedTrefleDetail
import com.verdura.app.model.PlantInfo
import com.verdura.app.util.ApiConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

data class PaginatedPlants(
    val plants: List<PlantInfo>,
    val lastPage: Int
)

class PlantRepository(
    private val plantInfoDao: PlantInfoDao,
    private val detailCacheDao: PlantDetailCacheDao? = null,
    private val trefleCacheDao: TrefleDetailCacheDao? = null,
    private val apiService: PlantApiService = RetrofitClient.plantApiService,
    private val trefleService: TrefleApiService = RetrofitClient.trefleApiService
) {
    companion object {
        private const val PAGE_SIZE = 60
        private const val CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L
        private const val DETAIL_CACHE_DURATION_MS = 7 * 24 * 60 * 60 * 1000L
        private const val DETAIL_CACHE_SIZE = 50
        private val perenualCache = LruCache<Int, PlantDetailResponse>(DETAIL_CACHE_SIZE)
        private val gson = Gson()

        val genusCareDefaults = mapOf(
            "abelia" to Triple("Perennial", "Average", "Full sun"),
            "abelmoschus" to Triple("Annual", "Frequent", "Full sun"),
            "abies" to Triple("Perennial", "Average", "Part shade"),
            "abutilon" to Triple("Perennial", "Average", "Bright indirect"),
            "acacia" to Triple("Perennial", "Minimum", "Full sun"),
            "acalypha" to Triple("Perennial", "Average", "Bright indirect"),
            "acanthus" to Triple("Perennial", "Average", "Part shade"),
            "acer" to Triple("Perennial", "Average", "Full sun"),
            "achillea" to Triple("Perennial", "Minimum", "Full sun"),
            "acorus" to Triple("Perennial", "Frequent", "Part shade"),
            "actinidia" to Triple("Perennial", "Average", "Full sun"),
            "adenium" to Triple("Perennial", "Minimum", "Full sun"),
            "adiantum" to Triple("Perennial", "Frequent", "Low light"),
            "aeonium" to Triple("Perennial", "Minimum", "Bright indirect"),
            "aesculus" to Triple("Perennial", "Average", "Full sun"),
            "agapanthus" to Triple("Perennial", "Average", "Full sun"),
            "agave" to Triple("Perennial", "Minimum", "Full sun"),
            "ageratum" to Triple("Annual", "Average", "Full sun"),
            "aglaonema" to Triple("Perennial", "Average", "Low light"),
            "ailanthus" to Triple("Perennial", "Minimum", "Full sun"),
            "ajuga" to Triple("Perennial", "Average", "Part shade"),
            "albizia" to Triple("Perennial", "Average", "Full sun"),
            "alcea" to Triple("Biennial", "Average", "Full sun"),
            "alchemilla" to Triple("Perennial", "Average", "Part shade"),
            "allium" to Triple("Perennial", "Average", "Full sun"),
            "alocasia" to Triple("Perennial", "Frequent", "Bright indirect"),
            "aloe" to Triple("Perennial", "Minimum", "Bright indirect"),
            "aloysia" to Triple("Perennial", "Average", "Full sun"),
            "alpinia" to Triple("Perennial", "Frequent", "Part shade"),
            "alstroemeria" to Triple("Perennial", "Average", "Full sun"),
            "amaranthus" to Triple("Annual", "Average", "Full sun"),
            "amelanchier" to Triple("Perennial", "Average", "Full sun"),
            "ananas" to Triple("Perennial", "Minimum", "Bright indirect"),
            "anemone" to Triple("Perennial", "Average", "Part shade"),
            "anethum" to Triple("Annual", "Average", "Full sun"),
            "angelica" to Triple("Biennial", "Frequent", "Part shade"),
            "anthurium" to Triple("Perennial", "Frequent", "Bright indirect"),
            "antirrhinum" to Triple("Annual", "Average", "Full sun"),
            "aquilegia" to Triple("Perennial", "Average", "Part shade"),
            "araucaria" to Triple("Perennial", "Average", "Bright indirect"),
            "arbutus" to Triple("Perennial", "Minimum", "Full sun"),
            "arctostaphylos" to Triple("Perennial", "Minimum", "Full sun"),
            "ardisia" to Triple("Perennial", "Average", "Part shade"),
            "areca" to Triple("Perennial", "Average", "Bright indirect"),
            "aristolochia" to Triple("Perennial", "Average", "Part shade"),
            "armeria" to Triple("Perennial", "Minimum", "Full sun"),
            "artemisia" to Triple("Perennial", "Minimum", "Full sun"),
            "asparagus" to Triple("Perennial", "Average", "Bright indirect"),
            "aspidistra" to Triple("Perennial", "Average", "Low light"),
            "asplenium" to Triple("Perennial", "Frequent", "Low light"),
            "aster" to Triple("Perennial", "Average", "Full sun"),
            "astilbe" to Triple("Perennial", "Frequent", "Part shade"),
            "aucuba" to Triple("Perennial", "Average", "Part shade"),
            "azalea" to Triple("Perennial", "Average", "Part shade"),
            "begonia" to Triple("Perennial", "Average", "Bright indirect"),
            "berberis" to Triple("Perennial", "Average", "Full sun"),
            "betula" to Triple("Perennial", "Average", "Full sun"),
            "bougainvillea" to Triple("Perennial", "Minimum", "Full sun"),
            "buddleja" to Triple("Perennial", "Average", "Full sun"),
            "buxus" to Triple("Perennial", "Average", "Part shade"),
            "caladium" to Triple("Perennial", "Frequent", "Bright indirect"),
            "calathea" to Triple("Perennial", "Frequent", "Low light"),
            "calendula" to Triple("Annual", "Average", "Full sun"),
            "callistemon" to Triple("Perennial", "Average", "Full sun"),
            "camellia" to Triple("Perennial", "Average", "Part shade"),
            "campanula" to Triple("Perennial", "Average", "Full sun"),
            "canna" to Triple("Perennial", "Frequent", "Full sun"),
            "capsicum" to Triple("Annual", "Average", "Full sun"),
            "carex" to Triple("Perennial", "Average", "Part shade"),
            "carpinus" to Triple("Perennial", "Average", "Full sun"),
            "catalpa" to Triple("Perennial", "Average", "Full sun"),
            "cedrus" to Triple("Perennial", "Minimum", "Full sun"),
            "celosia" to Triple("Annual", "Average", "Full sun"),
            "cercis" to Triple("Perennial", "Average", "Full sun"),
            "chamaecyparis" to Triple("Perennial", "Average", "Full sun"),
            "chlorophytum" to Triple("Perennial", "Average", "Bright indirect"),
            "chrysanthemum" to Triple("Perennial", "Average", "Full sun"),
            "citrus" to Triple("Perennial", "Average", "Full sun"),
            "clematis" to Triple("Perennial", "Average", "Full sun"),
            "clivia" to Triple("Perennial", "Average", "Bright indirect"),
            "codiaeum" to Triple("Perennial", "Average", "Bright indirect"),
            "cornus" to Triple("Perennial", "Average", "Full sun"),
            "cosmos" to Triple("Annual", "Minimum", "Full sun"),
            "cotoneaster" to Triple("Perennial", "Average", "Full sun"),
            "crassula" to Triple("Perennial", "Minimum", "Bright indirect"),
            "crataegus" to Triple("Perennial", "Average", "Full sun"),
            "cryptomeria" to Triple("Perennial", "Average", "Full sun"),
            "cupressus" to Triple("Perennial", "Average", "Full sun"),
            "cycas" to Triple("Perennial", "Minimum", "Bright indirect"),
            "cyclamen" to Triple("Perennial", "Average", "Bright indirect"),
            "dahlia" to Triple("Perennial", "Average", "Full sun"),
            "daphne" to Triple("Perennial", "Average", "Part shade"),
            "delphinium" to Triple("Perennial", "Average", "Full sun"),
            "dianthus" to Triple("Perennial", "Average", "Full sun"),
            "dieffenbachia" to Triple("Perennial", "Average", "Bright indirect"),
            "digitalis" to Triple("Biennial", "Average", "Part shade"),
            "dracaena" to Triple("Perennial", "Minimum", "Bright indirect"),
            "echinacea" to Triple("Perennial", "Minimum", "Full sun"),
            "echeveria" to Triple("Perennial", "Minimum", "Full sun"),
            "epipremnum" to Triple("Perennial", "Average", "Low light"),
            "erica" to Triple("Perennial", "Average", "Full sun"),
            "eucalyptus" to Triple("Perennial", "Minimum", "Full sun"),
            "euonymus" to Triple("Perennial", "Average", "Full sun"),
            "euphorbia" to Triple("Perennial", "Minimum", "Full sun"),
            "fagus" to Triple("Perennial", "Average", "Full sun"),
            "ficus" to Triple("Perennial", "Average", "Bright indirect"),
            "forsythia" to Triple("Perennial", "Average", "Full sun"),
            "fraxinus" to Triple("Perennial", "Average", "Full sun"),
            "fuchsia" to Triple("Perennial", "Average", "Part shade"),
            "gardenia" to Triple("Perennial", "Average", "Bright indirect"),
            "ginkgo" to Triple("Perennial", "Average", "Full sun"),
            "gladiolus" to Triple("Perennial", "Average", "Full sun"),
            "hedera" to Triple("Perennial", "Average", "Part shade"),
            "helianthus" to Triple("Annual", "Average", "Full sun"),
            "helleborus" to Triple("Perennial", "Average", "Part shade"),
            "hemerocallis" to Triple("Perennial", "Average", "Full sun"),
            "hibiscus" to Triple("Perennial", "Average", "Full sun"),
            "hosta" to Triple("Perennial", "Average", "Part shade"),
            "hydrangea" to Triple("Perennial", "Frequent", "Part shade"),
            "ilex" to Triple("Perennial", "Average", "Part shade"),
            "impatiens" to Triple("Annual", "Frequent", "Part shade"),
            "iris" to Triple("Perennial", "Average", "Full sun"),
            "jasminum" to Triple("Perennial", "Average", "Full sun"),
            "juniperus" to Triple("Perennial", "Minimum", "Full sun"),
            "kalanchoe" to Triple("Perennial", "Minimum", "Bright indirect"),
            "lantana" to Triple("Perennial", "Minimum", "Full sun"),
            "lavandula" to Triple("Perennial", "Minimum", "Full sun"),
            "ligustrum" to Triple("Perennial", "Average", "Full sun"),
            "lilium" to Triple("Perennial", "Average", "Full sun"),
            "liquidambar" to Triple("Perennial", "Average", "Full sun"),
            "liriope" to Triple("Perennial", "Average", "Part shade"),
            "lonicera" to Triple("Perennial", "Average", "Full sun"),
            "magnolia" to Triple("Perennial", "Average", "Full sun"),
            "malus" to Triple("Perennial", "Average", "Full sun"),
            "monstera" to Triple("Perennial", "Average", "Bright indirect"),
            "musa" to Triple("Perennial", "Frequent", "Full sun"),
            "nandina" to Triple("Perennial", "Average", "Full sun"),
            "narcissus" to Triple("Perennial", "Average", "Full sun"),
            "nerium" to Triple("Perennial", "Minimum", "Full sun"),
            "olea" to Triple("Perennial", "Minimum", "Full sun"),
            "paeonia" to Triple("Perennial", "Average", "Full sun"),
            "pelargonium" to Triple("Perennial", "Average", "Full sun"),
            "peperomia" to Triple("Perennial", "Average", "Bright indirect"),
            "petunia" to Triple("Annual", "Average", "Full sun"),
            "philodendron" to Triple("Perennial", "Average", "Bright indirect"),
            "photinia" to Triple("Perennial", "Average", "Full sun"),
            "picea" to Triple("Perennial", "Average", "Full sun"),
            "pieris" to Triple("Perennial", "Average", "Part shade"),
            "pinus" to Triple("Perennial", "Minimum", "Full sun"),
            "pittosporum" to Triple("Perennial", "Average", "Full sun"),
            "platanus" to Triple("Perennial", "Average", "Full sun"),
            "plumeria" to Triple("Perennial", "Average", "Full sun"),
            "populus" to Triple("Perennial", "Average", "Full sun"),
            "primula" to Triple("Perennial", "Average", "Part shade"),
            "prunus" to Triple("Perennial", "Average", "Full sun"),
            "pyrus" to Triple("Perennial", "Average", "Full sun"),
            "quercus" to Triple("Perennial", "Average", "Full sun"),
            "rhododendron" to Triple("Perennial", "Average", "Part shade"),
            "rosa" to Triple("Perennial", "Average", "Full sun"),
            "rosmarinus" to Triple("Perennial", "Minimum", "Full sun"),
            "rudbeckia" to Triple("Perennial", "Average", "Full sun"),
            "salix" to Triple("Perennial", "Frequent", "Full sun"),
            "salvia" to Triple("Perennial", "Average", "Full sun"),
            "sambucus" to Triple("Perennial", "Average", "Full sun"),
            "sansevieria" to Triple("Perennial", "Minimum", "Low light"),
            "schefflera" to Triple("Perennial", "Average", "Bright indirect"),
            "sedum" to Triple("Perennial", "Minimum", "Full sun"),
            "spathiphyllum" to Triple("Perennial", "Average", "Low light"),
            "spiraea" to Triple("Perennial", "Average", "Full sun"),
            "strelitzia" to Triple("Perennial", "Average", "Full sun"),
            "syringa" to Triple("Perennial", "Average", "Full sun"),
            "taxus" to Triple("Perennial", "Average", "Part shade"),
            "thuja" to Triple("Perennial", "Average", "Full sun"),
            "tilia" to Triple("Perennial", "Average", "Full sun"),
            "tradescantia" to Triple("Perennial", "Average", "Bright indirect"),
            "tsuga" to Triple("Perennial", "Average", "Part shade"),
            "tulipa" to Triple("Perennial", "Average", "Full sun"),
            "ulmus" to Triple("Perennial", "Average", "Full sun"),
            "viburnum" to Triple("Perennial", "Average", "Full sun"),
            "vitis" to Triple("Perennial", "Average", "Full sun"),
            "weigela" to Triple("Perennial", "Average", "Full sun"),
            "wisteria" to Triple("Perennial", "Average", "Full sun"),
            "yucca" to Triple("Perennial", "Minimum", "Full sun"),
            "zamioculcas" to Triple("Perennial", "Minimum", "Low light"),
            "zinnia" to Triple("Annual", "Average", "Full sun"),
        )
    }

    fun searchPlants(query: String): Flow<List<PlantInfo>> = flow {
        val cached = plantInfoDao.searchPlants(query)
        cached.collect { localResults ->
            if (localResults.isNotEmpty()) {
                emit(localResults)
            }
        }
    }

    /**
     * For search queries: always search Room locally (no Perenual API call).
     * For initial load (no query): serve from Room cache if fresh, otherwise hit Perenual once.
     * If Perenual fails, serve stale Room data.
     */
    suspend fun fetchPlantsWithCache(
        query: String? = null,
        page: Int = 1
    ): Result<PaginatedPlants> {
        if (!query.isNullOrBlank()) {
            val results = plantInfoDao.searchPlantsOnce(query, PAGE_SIZE)
            return Result.success(PaginatedPlants(results, 1))
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

    suspend fun fetchPlantsFromApi(
        query: String? = null,
        page: Int = 1
    ): Result<PaginatedPlants> {
        return try {
            val response = apiService.getPlantList(ApiConfig.getPlantApiKey(), page, query)
            val plants = response.data.map { summary ->
                PlantInfo(
                    id = summary.id,
                    commonName = summary.commonName,
                    scientificName = summary.scientificName?.joinToString(", "),
                    cycle = summary.cycle?.takeUnless { it.startsWith("Upgrade") },
                    watering = summary.watering?.takeUnless { it.startsWith("Upgrade") },
                    sunlight = summary.sunlight
                        ?.filter { !it.startsWith("Upgrade") }
                        ?.joinToString(", ")
                        ?.takeIf { it.isNotBlank() },
                    imageUrl = summary.defaultImage?.regularUrl
                        ?: summary.defaultImage?.mediumUrl
                        ?: summary.defaultImage?.smallUrl
                )
            }
            plantInfoDao.insertAll(plants)
            val totalPages = response.lastPage ?: 1
            Result.success(PaginatedPlants(plants, totalPages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchPlantDetails(plantId: Int): Result<PlantDetailResponse> {
        perenualCache.get(plantId)?.let { cached ->
            backfillPlantInfo(plantId, cached)
            return Result.success(cached)
        }

        val minTimestamp = System.currentTimeMillis() - DETAIL_CACHE_DURATION_MS
        try {
            detailCacheDao?.get(plantId, minTimestamp)?.let { cached ->
                val detail = gson.fromJson(cached.detailJson, PlantDetailResponse::class.java)
                perenualCache.put(plantId, detail)
                backfillPlantInfo(plantId, detail)
                return Result.success(detail)
            }
        } catch (_: Exception) { }

        return try {
            val response = apiService.getPlantDetails(plantId, ApiConfig.getPlantApiKey())
            perenualCache.put(plantId, response)
            backfillPlantInfo(plantId, response)
            try {
                detailCacheDao?.insert(
                    CachedPlantDetail(plantId, gson.toJson(response))
                )
            } catch (_: Exception) { }
            Result.success(response)
        } catch (e: Exception) {
            try {
                detailCacheDao?.get(plantId, 0)?.let { stale ->
                    val detail = gson.fromJson(stale.detailJson, PlantDetailResponse::class.java)
                    perenualCache.put(plantId, detail)
                    backfillPlantInfo(plantId, detail)
                    return Result.success(detail)
                }
            } catch (_: Exception) { }
            Result.failure(e)
        }
    }

    private suspend fun backfillPlantInfo(plantId: Int, detail: PlantDetailResponse) {
        try {
            val cycle = detail.cycle?.takeIf { it.isNotBlank() && !it.startsWith("Upgrade") }
            val watering = detail.watering?.takeIf { it.isNotBlank() && !it.startsWith("Upgrade") }
            val sunlight = detail.sunlight
                ?.filter { it.isNotBlank() && !it.startsWith("Upgrade") }
                ?.joinToString(", ")
                ?.takeIf { it.isNotBlank() }
            if (cycle != null || watering != null || sunlight != null) {
                plantInfoDao.updateDetailFields(plantId, cycle, watering, sunlight)
            }
        } catch (_: Exception) { }
    }

    suspend fun fetchTrefleDetails(
        scientificName: String,
        commonName: String? = null
    ): Result<TrefleSpeciesDetail> {
        val cacheKey = scientificName.split(",").first().trim()
        if (cacheKey.isBlank()) return Result.failure(Exception("No scientific name"))

        try {
            trefleCacheDao?.get(cacheKey)?.let { cached ->
                val detail = gson.fromJson(cached.detailJson, TrefleSpeciesDetail::class.java)
                return Result.success(detail)
            }
        } catch (_: Exception) { }

        val token = ApiConfig.getTrefleToken()
        val queries = buildList {
            add(cacheKey)
            val genus = cacheKey.split(" ").firstOrNull()
            if (!genus.isNullOrBlank() && genus != cacheKey) add(genus)
            if (!commonName.isNullOrBlank()) add(commonName)
        }

        for (query in queries) {
            try {
                val searchResult = trefleService.searchSpecies(token, query)
                if (searchResult.data.isEmpty()) continue
                val match = searchResult.data.firstOrNull {
                    it.scientificName.equals(cacheKey, ignoreCase = true)
                } ?: searchResult.data.firstOrNull {
                    it.scientificName?.startsWith(
                        cacheKey.split(" ").first(), ignoreCase = true
                    ) == true
                } ?: searchResult.data.first()

                val detail = trefleService.getSpeciesDetails(match.id, token).data
                try {
                    trefleCacheDao?.insert(CachedTrefleDetail(cacheKey, gson.toJson(detail)))
                } catch (_: Exception) { }
                return Result.success(detail)
            } catch (_: Exception) { }
        }
        return Result.failure(Exception("No Trefle match"))
    }

    suspend fun backfillPlantInfoFromTrefle(plantId: Int, detail: TrefleSpeciesDetail, scientificName: String? = null) {
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

    private fun lightToLabel(light: Int): String = when {
        light <= 3 -> "Low light"
        light <= 5 -> "Part shade"
        light <= 7 -> "Bright indirect"
        else -> "Full sun"
    }

    private fun humidityToLabel(humidity: Int): String = when {
        humidity <= 2 -> "Minimum"
        humidity <= 4 -> "Average"
        humidity <= 6 -> "Frequent"
        else -> "Abundant"
    }

    /**
     * Backfill PlantInfo rows from any already-cached detail data in Room
     * (both Perenual and Trefle caches). No API calls made.
     */
    suspend fun backfillFromCachedDetails(): Int {
        val missing = plantInfoDao.getPlantsMissingCareData(60)
        if (missing.isEmpty()) return 0
        var enriched = 0
        for (plant in missing) {
            try {
                detailCacheDao?.get(plant.id, 0)?.let { cached ->
                    val detail = gson.fromJson(cached.detailJson, PlantDetailResponse::class.java)
                    backfillPlantInfo(plant.id, detail)
                    enriched++
                }
            } catch (_: Exception) { }

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

    /**
     * Fetches care data from Trefle for plants missing cycle/watering/sunlight.
     * Calls [onBatchReady] after every [batchSize] successful enrichments so
     * the UI can refresh incrementally.
     */
    suspend fun prefetchCareFromTrefle(
        limit: Int = 60,
        batchSize: Int = 5,
        onBatchReady: suspend () -> Unit = {}
    ) {
        val plants = plantInfoDao.getPlantsMissingCareData(limit)
        var batchCount = 0
        for (plant in plants) {
            val sciName = plant.scientificName ?: continue
            try {
                val result = withTimeoutOrNull(15_000L) {
                    fetchTrefleDetails(sciName, plant.commonName)
                } ?: continue
                result.onSuccess { detail ->
                    backfillPlantInfoFromTrefle(plant.id, detail, plant.scientificName)
                    batchCount++
                    if (batchCount % batchSize == 0) onBatchReady()
                }
            } catch (_: Exception) { }
            delay(200)
        }
        if (batchCount % batchSize != 0) onBatchReady()
    }

    fun getCachedPlants(): Flow<List<PlantInfo>> {
        return plantInfoDao.getAllPlants()
    }

    suspend fun getCachedPlantsOnce(query: String? = null): List<PlantInfo> {
        return if (query.isNullOrBlank()) {
            plantInfoDao.getAllPlantsOnce()
        } else {
            plantInfoDao.searchPlantsOnce(query)
        }
    }

    suspend fun isCacheValid(): Boolean {
        val count = plantInfoDao.getCount()
        return count > 0
    }
}
