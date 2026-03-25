package com.verdura.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.verdura.app.model.PlantInfo
import com.verdura.app.repository.PlantRepository
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

    init {
        loadPlants()
    }

    fun loadPlants(query: String? = null) {
        currentPage = 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = plantRepository.fetchPlantsFromApi(query, currentPage)
            result.fold(
                onSuccess = { plants ->
                    _plants.value = plants
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    viewModelScope.launch {
                        plantRepository.getCachedPlants().collect { cached ->
                            if (cached.isNotEmpty()) _plants.value = cached
                        }
                    }
                }
            )
        }
    }

    fun searchPlants(query: String) {
        if (query.isBlank()) {
            loadPlants()
            return
        }
        loadPlants(query)
    }

    fun loadNextPage(query: String? = null) {
        currentPage++
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = plantRepository.fetchPlantsFromApi(query, currentPage)
            result.fold(
                onSuccess = { newPlants ->
                    _plants.value = _plants.value + newPlants
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { e ->
                    currentPage--
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
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
    val error: String? = null
)
