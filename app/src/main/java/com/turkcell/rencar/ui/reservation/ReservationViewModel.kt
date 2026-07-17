package com.turkcell.rencar.ui.reservation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.data.repository.ReservationRepository
import com.turkcell.rencar.data.repository.VehicleRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toMessage()) }
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
                    _uiState.update { it.copy(isReserving = false, errorMessage = e.toReserveMessage()) }
                }
        }
    }

    /**
     * POST /rentals (DAILY): rezervasyon tutulduktan hemen sonra kiralamayı açar. API bu planda
     * kaydı anında ACTIVE yapar ve fiyatı kilitler; başarıda aktif yolculuk ekranına geçilir.
     *
     * Başarısız olursa rezervasyon askıda kalır (araç 15 dk RESERVED); kullanıcı butona tekrar
     * basarsa POST /reservations 409 döner ve o mesaj gösterilir — bu yüzden hata metni
     * ayrıştırılır ([toStartRentalMessage]).
     */
    private suspend fun startDailyRental() {
        rentalRepository.createDailyRental(vehicleId)
            .onSuccess { rental ->
                _uiState.update { it.copy(isReserving = false, startedRentalId = rental.id) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isReserving = false, errorMessage = e.toStartRentalMessage()) }
            }
    }

    /** Ekran, reserved bayrağını navigasyonda tüketince çağrılır (tekrar geçişi önler). */
    fun onReservedHandled() {
        _uiState.update { it.copy(reserved = false) }
    }

    /** Ekran, startedRentalId'yi navigasyonda tüketince çağrılır (tekrar geçişi önler). */
    fun onRentalStartedHandled() {
        _uiState.update { it.copy(startedRentalId = null) }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Rezervasyon için ehliyet onayınız gerekli."
            404 -> "Bu araç şu anda müsait değil."
            else -> "Araç yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toReserveMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Rezervasyon için ehliyet onayınız gerekli."
            404 -> "Araç bulunamadı."
            409 -> "Bu araç artık müsait değil veya zaten aktif bir rezervasyonunuz var."
            else -> "Rezervasyon oluşturulamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    /** Günlük planda POST /rentals hataları — rezervasyon alınmış, kiralama açılamamıştır. */
    private fun Throwable.toStartRentalMessage(): String = when (this) {
        is HttpException -> when (code()) {
            400 -> "Kiralama başlatılamadı: iade tarihi geçersiz."
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Kiralama için ehliyet onayınız gerekli."
            404 -> "Araç bulunamadı."
            409 -> "Zaten aktif bir kiralamanız var veya araç kiralanabilir değil."
            else -> "Kiralama başlatılamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
