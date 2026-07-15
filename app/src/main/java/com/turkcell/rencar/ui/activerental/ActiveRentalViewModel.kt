package com.turkcell.rencar.ui.activerental

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.ActiveRentalUi
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Aktif Yolculuk ekranının tek durum kaynağı (§4.4).
 *
 * Simülasyon otomatik başlamaz: "Kilitle / Aç" ilk basışında ([lockToggle]) üç canlı iş başlar
 * (hepsi [viewModelScope]'ta; ekran/VM ölünce durur):
 *  1. **Poll** — GET /rentals/active'i periyodik çeker; anlık ücret/mesafe/geçen süre buradan gelir.
 *  2. **Sayaç** — geçen süreyi ekranda 1 sn'de bir akıtır; her poll'de sunucu değeriyle resync eder.
 *  3. **Socket** — [RentalRepository.vehiclePositionStream] ile aracın canlı konumunu toplar (harita).
 *
 * "Kiralamayı Bitir" (POST /rentals/{id}/finish) başarılı olunca üç iş de durur ve ekran, ücret
 * dökümüyle "bitti" durumunda kalır (ödeme ayrı bir adımdır — bu iş kapsamında değil).
 * Ayrı domain/UseCase katmanı yoktur (decisions.md).
 */
@HiltViewModel
class ActiveRentalViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentalRepository: RentalRepository,
) : ViewModel() {

    private val rentalId: String =
        savedStateHandle.get<String>(RencarDestinations.ACTIVE_RENTAL_ARG_RENTAL_ID).orEmpty()

    private val _uiState = MutableStateFlow(ActiveRentalUiState(rentalId = rentalId))
    val uiState: StateFlow<ActiveRentalUiState> = _uiState.asStateFlow()

    // Geçen süre sayacı için çapa: son poll'deki sunucu değeri + o andaki cihaz saati.
    private var elapsedBaseSeconds: Long = 0L
    private var elapsedAnchorMillis: Long = 0L

    private var pollJob: Job? = null
    private var tickerJob: Job? = null
    private var socketJob: Job? = null

    // Simülasyon (poll+sayaç+socket) artık init'te DEĞİL, "Kilitle / Aç" ilk basışında başlar.

    fun onIntent(intent: ActiveRentalIntent) {
        when (intent) {
            ActiveRentalIntent.Retry -> retry()
            ActiveRentalIntent.FinishClicked -> finishRental()
            ActiveRentalIntent.LockToggle -> lockToggle()
        }
    }

    /**
     * "Kilitle / Aç": kilit görselini çevirir. **İlk basışta** simülasyonu başlatır (bir sonraki
     * poll gelene kadar tam-ekran "başlatılıyor" spinner'ı için isLoading=true). Kilit/aç API ucu
     * yoktur (decisions.md/§2.2: uydurulmaz); ağ çağrısı yapılmaz.
     */
    private fun lockToggle() {
        val alreadyStarted = _uiState.value.started
        _uiState.update {
            it.copy(
                locked = !it.locked,
                started = true,
                isLoading = if (alreadyStarted) it.isLoading else true,
            )
        }
        if (!alreadyStarted) startSimulation()
    }

    /** Üç canlı işi başlatır: GET /rentals/active poll + geçen süre sayacı + Socket.IO konum akışı. */
    private fun startSimulation() {
        startPolling()
        startTicker()
        observeVehicle()
    }

    /** GET /rentals/active döngüsü: ilk turda yükleme/hata, sonraki turlarda sessiz güncelleme. */
    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var first = true
            while (isActive) {
                rentalRepository.getActiveRental()
                    .onSuccess { active ->
                        applyActive(active)
                        _uiState.update { it.copy(isLoading = false, loadError = null) }
                    }
                    .onFailure { e ->
                        // İlk yükleme başarısızsa tam ekran hata; sonraki geçici hatalar yok sayılır
                        // (ekran son bilinen değerleri gösterir).
                        if (first) _uiState.update { it.copy(isLoading = false, loadError = e.toLoadMessage()) }
                    }
                first = false
                if (_uiState.value.isFinished) break
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /** Geçen süreyi 1 sn'de bir akıtır: base + (şimdi − çapa). Poll her başarıda çapayı tazeler. */
    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (isActive) {
                if (elapsedAnchorMillis > 0L) {
                    val extra = (System.currentTimeMillis() - elapsedAnchorMillis) / 1000
                    _uiState.update { it.copy(elapsedSeconds = elapsedBaseSeconds + extra) }
                }
                delay(1_000L)
            }
        }
    }

    /** Aracın canlı konumunu toplar (Socket.IO). Akış sessizse (kare gelmezse) harita çizilmez. */
    private fun observeVehicle() {
        socketJob?.cancel()
        socketJob = viewModelScope.launch {
            rentalRepository.vehiclePositionStream().collect { point ->
                _uiState.update { it.copy(vehiclePoint = point) }
            }
        }
    }

    /** Poll cevabını state'e ve süre çapasına uygular. */
    private fun applyActive(active: ActiveRentalUi) {
        elapsedBaseSeconds = active.elapsedSeconds
        elapsedAnchorMillis = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                rentalId = active.rentalId.ifEmpty { it.rentalId },
                vehicleTitle = active.vehicleTitle,
                vehiclePlate = active.vehiclePlate,
                planLabel = active.planLabel,
                startedAtLabel = active.startedAtLabel,
                startFee = active.startFee,
                distanceKm = active.distanceKm,
                currentCost = active.currentCost,
                elapsedSeconds = active.elapsedSeconds,
            )
        }
    }

    /** POST /rentals/{id}/finish: yolculuğu + simülasyonu bitirir; başarıda ekran özetle kalır. */
    private fun finishRental() {
        val state = _uiState.value
        if (state.isFinishing || state.isFinished) return
        val id = state.rentalId
        if (id.isEmpty()) return

        _uiState.update { it.copy(isFinishing = true, finishError = null) }
        viewModelScope.launch {
            rentalRepository.finishRental(id)
                .onSuccess { receipt ->
                    stopLiveUpdates()
                    _uiState.update {
                        it.copy(
                            isFinishing = false,
                            receipt = receipt,
                            vehiclePoint = null,
                            elapsedSeconds = receipt.elapsedSeconds,
                            distanceKm = receipt.distanceKm,
                            currentCost = receipt.totalPrice,
                            locked = true,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isFinishing = false, finishError = e.toFinishMessage()) }
                }
        }
    }

    private fun retry() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        startPolling()
    }

    /** Canlı işleri durdurur (finish sonrası): poll + sayaç + socket. */
    private fun stopLiveUpdates() {
        pollJob?.cancel(); pollJob = null
        tickerJob?.cancel(); tickerJob = null
        socketJob?.cancel(); socketJob = null
    }

    private fun Throwable.toLoadMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            404 -> "Aktif yolculuk bulunamadı."
            else -> "Yolculuk durumu alınamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toFinishMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Bu yolculuk size ait değil."
            404 -> "Yolculuk bulunamadı."
            409 -> "Yolculuk bitirilemedi: zaten bitmiş olabilir."
            else -> "Yolculuk bitirilemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private companion object {
        const val POLL_INTERVAL_MS = 4_000L
    }
}
