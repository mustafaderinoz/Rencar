package com.turkcell.rencar.ui.reservation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.data.repository.ReservationRepository
import com.turkcell.rencar.data.repository.VehicleRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Rezervasyon Onayı ekranının tek durum kaynağı (§4.4).
 *
 * Araç detayından iletilen vehicleId path argümanını [SavedStateHandle] ile okur; açılışta
 * GET /vehicles/{id} + GET /vehicles/{id}/quote çeker. Plan çipi değişince quote yeniden alınır.
 *
 * "Rezervasyonu Tamamla" POST /reservations çağırır; sonrası plana göre AYRIŞIR:
 * - **Dakikalık/Saatlik:** kiralama foto ekranında açılır (POST /rentals orada) →
 *   [ReservationUiState.reserved] bayrağı verilir.
 * - **Günlük:** foto adımı YOKTUR (API kaydı anında ACTIVE yapar), bu yüzden kiralama BURADA açılır
 *   (POST /rentals + endDate) → [ReservationUiState.startedRentalId] verilir.
 *
 * Navigasyon ekran katmanındadır. Ayrı domain/UseCase katmanı yoktur (decisions.md).
 */
@HiltViewModel
class ReservationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val vehicleRepository: VehicleRepository,
    private val reservationRepository: ReservationRepository,
    private val rentalRepository: RentalRepository,
) : ViewModel() {

    private val vehicleId: String =
        savedStateHandle.get<String>(RencarDestinations.RESERVATION_ARG_VEHICLE_ID).orEmpty()

    private val _uiState = MutableStateFlow(ReservationUiState())
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    // Plan hızlı değişince eski quote isteğini iptal etmek için son quote işi tutulur.
    private var quoteJob: Job? = null

    init {
        load()
    }

    fun onIntent(intent: ReservationIntent) {
        when (intent) {
            ReservationIntent.Load, ReservationIntent.Retry -> load()
            is ReservationIntent.PlanSelected -> selectPlan(intent.plan)
            ReservationIntent.TermsToggled ->
                _uiState.update { it.copy(termsAccepted = !it.termsAccepted) }
            ReservationIntent.ReserveClicked -> reserve()
            // Navigasyon Screen katmanında ele alınır (§4.6).
            ReservationIntent.BackClicked -> Unit

            // Ekran geçişi yaptı → bayrakları tüket (tekrar geçişi önler).
            ReservationIntent.ReservedHandled -> _uiState.update { it.copy(reserved = false) }
            ReservationIntent.RentalStartedHandled ->
                _uiState.update { it.copy(startedRentalId = null) }
        }
    }

    /** GET /vehicles/{id}: araç kartını yükler; başarıda seçili plana göre quote'u çeker. */
    private fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            vehicleRepository.getVehicle(vehicleId)
                .onSuccess { vehicle ->
                    _uiState.update { it.copy(isLoading = false, vehicle = vehicle) }
                    loadQuote(_uiState.value.selectedPlan)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.RESERVATION_LOAD),
                        )
                    }
                }
        }
    }

    /** Plan çipi seçildi: durumu güncelle ve o plana ait quote'u yeniden çek. */
    private fun selectPlan(plan: RentalPlan) {
        if (_uiState.value.selectedPlan == plan) return
        _uiState.update { it.copy(selectedPlan = plan) }
        loadQuote(plan)
    }

    /** GET /vehicles/{id}/quote: seçili plan + tahmini süre için fiyat dökümünü çeker. */
    private fun loadQuote(plan: RentalPlan) {
        quoteJob?.cancel()
        _uiState.update { it.copy(isQuoteLoading = true) }
        quoteJob = viewModelScope.launch {
            reservationRepository.getQuote(vehicleId, plan.apiPlan, plan.estimateMinutes)
                .onSuccess { quote ->
                    // Yanıt gelene kadar plan değişmiş olabilir; yalnız güncel plana aitse uygula.
                    _uiState.update {
                        if (it.selectedPlan == plan) it.copy(isQuoteLoading = false, quote = quote) else it
                    }
                }
                .onFailure {
                    _uiState.update {
                        if (it.selectedPlan == plan) it.copy(isQuoteLoading = false, quote = null) else it
                    }
                }
        }
    }

    /**
     * POST /reservations: aracı 15 dk ücretsiz tutar. Günlük planda kiralama da hemen açılır
     * (foto adımı yok); diğer planlarda foto ekranına geçiş bayrağı verilir.
     */
    private fun reserve() {
        val state = _uiState.value
        if (!state.canReserve) return
        val plan = state.selectedPlan

        _uiState.update { it.copy(isReserving = true, errorMessage = null) }
        viewModelScope.launch {
            reservationRepository.reserve(vehicleId)
                .onSuccess {
                    if (plan == RentalPlan.DAILY) {
                        startDailyRental()
                    } else {
                        _uiState.update { it.copy(isReserving = false, reserved = true) }
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isReserving = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.RESERVATION_CREATE),
                        )
                    }
                }
        }
    }

    /**
     * POST /rentals (DAILY): rezervasyon tutulduktan hemen sonra kiralamayı açar. API bu planda
     * kaydı anında ACTIVE yapar ve fiyatı kilitler; başarıda aktif yolculuk ekranına geçilir.
     *
     * Başarısız olursa rezervasyon askıda kalır (araç 15 dk RESERVED); kullanıcı butona tekrar
     * basarsa POST /reservations 409 döner ve o mesaj gösterilir — bu yüzden hata metni ayrı bir
     * bağlamla ([ErrorContext.RESERVATION_RENT]) çözülür.
     */
    private suspend fun startDailyRental() {
        rentalRepository.createDailyRental(vehicleId)
            .onSuccess { rental ->
                _uiState.update { it.copy(isReserving = false, startedRentalId = rental.id) }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        isReserving = false,
                        errorMessage = e.toAppError().toUserMessage(ErrorContext.RESERVATION_RENT),
                    )
                }
            }
    }

}
