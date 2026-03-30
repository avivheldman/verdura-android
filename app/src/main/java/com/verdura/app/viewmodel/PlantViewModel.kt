package com.verdura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdura.app.model.PlantInfo
import com.verdura.app.repository.PlantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlantViewModel(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _plants = MutableStateFlow<List<PlantInfo>>(emptyList())
    val plants: StateFlow<List<PlantInfo>> = _plants.asStateFlow()

    private val _uiState = MutableStateFlow(PlantUiState())
    val uiState: StateFlow<PlantUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var lastPage = Int.MAX_VALUE
    private var currentQuery: String? = null
    private var loadJob: Job? = null
    private var paginationJob: Job? = null
    private var prefetchJob: Job? = null

    init {
        loadPlants()
    }

    fun loadPlants(query: String? = null) {
        loadJob?.cancel()
        prefetchJob?.cancel()
        currentPage = 1
        lastPage = Int.MAX_VALUE
        currentQuery = query
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = plantRepository.fetchPlantsWithCache(query, currentPage)
            result.fold(
                onSuccess = { (plants, totalPages) ->
                    lastPage = totalPages
                    _plants.value = rankByRelevance(plants, query)
                    _uiState.update { it.copy(isLoading = false, hasMorePages = currentPage < lastPage) }
                    enrichPlantData(query)
                },
                onFailure = { e ->
                    val cached = plantRepository.getCachedPlantsOnce(query)
                    if (cached.isNotEmpty()) {
                        _plants.value = rankByRelevance(cached, query)
                        _uiState.update { it.copy(isLoading = false, hasMorePages = false) }
                        enrichPlantData(query)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = e.message, hasMorePages = false) }
                    }
                }
            )
        }
    }

    private fun enrichPlantData(query: String?) {
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            val genusFilled = plantRepository.backfillFromGenusFallback()
            if (genusFilled > 0) refreshList(query)

            val backfilled = plantRepository.backfillFromCachedDetails()
            if (backfilled > 0) refreshList(query)

            delay(300)
            plantRepository.prefetchCareFromTrefle(
                limit = 30,
                batchSize = 5
            ) {
                refreshList(query)
            }
        }
    }

    private suspend fun refreshList(query: String?) {
        val updated = plantRepository.getCachedPlantsOnce(query)
        if (updated.isNotEmpty()) {
            _plants.value = rankByRelevance(updated, query)
        }
    }

    private fun rankByRelevance(plants: List<PlantInfo>, query: String?): List<PlantInfo> {
        if (query.isNullOrBlank()) return plants
        val q = query.lowercase()
        return plants.sortedWith(compareBy { plant ->
            val name = plant.commonName?.lowercase() ?: ""
            when {
                name.equals(q, ignoreCase = true) -> 0
                name.startsWith(q) -> 1
                name.contains(" $q") -> 2
                else -> 3
            }
        })
    }

    fun searchPlants(query: String) {
        if (query.isBlank()) {
            loadPlants()
            return
        }
        loadPlants(query)
    }

    fun loadNextPage() {
        if (paginationJob?.isActive == true) return
        if (currentPage >= lastPage) return

        currentPage++
        paginationJob = viewModelScope.launch {
            _uiState.update { it.copy(isPaginating = true, error = null) }
            val result = plantRepository.fetchPlantsFromApi(currentQuery, currentPage)
            result.fold(
                onSuccess = { (newPlants, totalPages) ->
                    lastPage = totalPages
                    _plants.value = _plants.value + newPlants
                    _uiState.update { it.copy(isPaginating = false, hasMorePages = currentPage < lastPage) }
                },
                onFailure = { e ->
                    currentPage--
                    _uiState.update { it.copy(isPaginating = false, error = e.message) }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class PlantUiState(
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null
)
