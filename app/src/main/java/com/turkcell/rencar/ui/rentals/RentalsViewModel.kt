package com.turkcell.rencar.ui.rentals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Kiralamalarım ekranının tek durum kaynağı (§4.4). Sekmeye her girişte iki çağrı paralel yüklenir:
 * GET /rentals (liste, kritik) ve GET /rentals/stats (başlık özeti; hatası sessizce yok sayılır).
 *
 * Yükleme init'te değil, [RentalsIntent.Load] ile tetiklenir (ekran her göründüğünde tazeler; yeni
 * tamamlanan kiralama böylece listede belirir). Listede veri varken tazeleme SESSİZDİR (spinner
 * çakması olmaz); yalnız ilk açılışta (veri yokken) tam ekran spinner gösterilir. Ayrı UseCase yoktur.
 */
@HiltViewModel
class RentalsViewModel @Inject constructor(
    private val rentalRepository: RentalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RentalsUiState())
    val uiState: StateFlow<RentalsUiState> = _uiState.asStateFlow()

    fun onIntent(intent: RentalsIntent) {
        when (intent) {
            RentalsIntent.Load -> load()
            RentalsIntent.Retry -> load()
        }
    }

    /** Liste (kritik) + özeti paralel yükler. Veri yokken tam ekran spinner; varken sessiz tazeler. */
    private fun load() {
        _uiState.update {
            it.copy(isLoading = it.rentals.isEmpty(), loadError = null)
        }
        viewModelScope.launch {
            val rentalsDeferred = async { rentalRepository.getMyRentals() }
            val statsDeferred = async { rentalRepository.getMonthlyStats() }

            rentalsDeferred.await()
                .onSuccess { rentals ->
                    val stats = statsDeferred.await().getOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            rentals = rentals,
                            // Özet hatası sessiz: eldeki değeri koru, yenisi geldiyse güncelle.
                            stats = stats ?: it.stats,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        // Eldeki liste varken (sessiz tazeleme) hata yut; yoksa tam ekran hata göster.
                        if (it.rentals.isNotEmpty()) it.copy(isLoading = false)
                        else it.copy(
                            isLoading = false,
                            loadError = e.toAppError().toUserMessage(ErrorContext.RENTALS_LIST),
                        )
                    }
                }
        }
    }
}
