package com.turkcell.rencar.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.data.repository.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AI Önerisi ViewModel: Kullanıcı sorgusunu alır ve AiRepository üzerinden öneri ister.
 */
@HiltViewModel
class AiRecommendationViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiRecommendationUiState())
    val uiState: StateFlow<AiRecommendationUiState> = _uiState.asStateFlow()

    fun onIntent(intent: AiRecommendationIntent, vehicles: List<VehicleUi>) {
        when (intent) {
            is AiRecommendationIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.query, error = null) }
            }
            AiRecommendationIntent.Submit -> {
                recommend(vehicles)
            }
            AiRecommendationIntent.Clear -> {
                _uiState.update { AiRecommendationUiState() }
            }
        }
    }

    private fun recommend(vehicles: List<VehicleUi>) {
        val query = _uiState.value.query
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            aiRepository.recommendVehicles(query, vehicles)
                .onSuccess { ids ->
                    _uiState.update { it.copy(isLoading = false, recommendedIds = ids) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Öneri alınamadı: ${e.message}") }
                }
        }
    }
}
