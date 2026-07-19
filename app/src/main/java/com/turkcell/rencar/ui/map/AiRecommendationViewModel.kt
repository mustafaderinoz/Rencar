package com.turkcell.rencar.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun onIntent(intent: AiRecommendationIntent) {
        when (intent) {
            is AiRecommendationIntent.QueryChanged ->
                _uiState.update { it.copy(query = intent.query, error = null) }

            is AiRecommendationIntent.VehiclesProvided ->
                _uiState.update { it.copy(vehicles = intent.vehicles) }

            AiRecommendationIntent.Submit -> recommend()

            AiRecommendationIntent.Clear ->
                _uiState.update { AiRecommendationUiState() }

            // Kapatma Screen katmanında ele alınır (§4.6).
            AiRecommendationIntent.Dismiss -> Unit
        }
    }

    private fun recommend() {
        val state = _uiState.value
        val query = state.query
        if (query.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            aiRepository.recommendVehicles(query, state.vehicles)
                .onSuccess { ids ->
                    _uiState.update { it.copy(isLoading = false, recommendedIds = ids) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = "Öneri alınamadı: ${e.message}") }
                }
        }
    }
}
