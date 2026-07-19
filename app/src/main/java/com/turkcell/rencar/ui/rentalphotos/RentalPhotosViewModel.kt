package com.turkcell.rencar.ui.rentalphotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.ResumableRentalUi
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.data.repository.ReservationRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.AppError
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Araç durumu (kiralama öncesi fotoğraf) ekranının tek durum kaynağı (§4.4).
 *
 * AKIŞ (decisions.md → "Rezervasyon → Foto → Başlat"): açılışta kiralama OLUŞTURULMAZ.
 * - **Normal mod:** GET /reservations/active ile aktif rezervasyonun 15 dk geri sayımı gösterilir;
 *   çekilen kareler yerelde tutulur. Rezervasyon "Başlat"a dek AKTİF kalır.
 * - **Kurtarma modu:** "Başlat" ortasında oluşmuş (ya da uygulama ölünce askıda kalmış) bir PREPARING
 *   kiralama varsa (rezervasyon o noktada CONVERTED) onu devralır; foto durumu sunucudan yüklenir.
 *
 * "Kiralamayı Başlat" zinciri: POST /rentals (rezervasyon CONVERTED) → yüklenmemiş kareler
 * POST /rentals/{id}/photos → POST /rentals/{id}/start. Zincir ortasında hata olursa kimlik/ilerleme
 * korunur; kullanıcı tekrar basınca kaldığı yerden sürer. "Rezervasyonu İptal Et" rezervasyonu
 * (veya oluşmuş kiralamayı) iptal eder; geri çıkış İPTAL ETMEZ (rezervasyon 15 dk / TTL sürer).
 *
 * Android API'lerine (kamera/FileProvider) dokunmaz; ekran çekim yapıp yolu intent ile iletir.
 * Ayrı domain/UseCase katmanı yoktur (decisions.md).
 */
@HiltViewModel
class RentalPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentalRepository: RentalRepository,
    private val reservationRepository: ReservationRepository,
) : ViewModel() {

    private val plan: String =
        savedStateHandle.get<String>(RencarDestinations.RENTAL_PHOTOS_ARG_PLAN).orEmpty()

    // "Başlat"ta POST /rentals için kullanılacak araç kimliği: rezervasyon/kurtarma yüklenince onun
    // aracıyla doldurulur (path argümanı yalnız yedek). Böylece 409 "rezervasyon yok" riskleri azalır.
    private var startVehicleId: String =
        savedStateHandle.get<String>(RencarDestinations.RENTAL_PHOTOS_ARG_VEHICLE_ID).orEmpty()

    private val _uiState = MutableStateFlow(RentalPhotosUiState())
    val uiState: StateFlow<RentalPhotosUiState> = _uiState.asStateFlow()

    // Aktif rezervasyonun geri sayımını süren yerel ticker (1 sn); yeni yüklemede/başlatmada iptal edilir.
    private var countdownJob: Job? = null

    init {
        load()
    }

    fun onIntent(intent: RentalPhotosIntent) {
        when (intent) {
            RentalPhotosIntent.Retry -> load()
            is RentalPhotosIntent.PhotoCaptured -> capturePhoto(intent.side, intent.path)
            RentalPhotosIntent.StartClicked -> startRental()
            RentalPhotosIntent.CancelClicked -> cancel()
            // Navigasyon Screen katmanında ele alınır (§4.6).
            RentalPhotosIntent.BackClicked -> Unit

            // Ekran geçişleri yaptı → bayrakları tüket (tekrar geçişi önler).
            RentalPhotosIntent.StartedHandled -> _uiState.update { it.copy(started = false) }
            RentalPhotosIntent.CancelledHandled -> _uiState.update { it.copy(cancelled = false) }
        }
    }

    /**
     * Açılış/tekrar-dene: önce PREPARING kiralama (kurtarma), yoksa aktif rezervasyon (normal mod)
     * yüklenir. İkisi de yoksa (404) rezervasyon süresi dolmuş sayılır; ağ/diğer hatada tekrar-dene.
     */
    private fun load() {
        countdownJob?.cancel()
        _uiState.update {
            it.copy(isLoading = true, loadError = null, reservationExpired = false, errorMessage = null)
        }
        viewModelScope.launch {
            // 1) Kurtarma: askıda PREPARING kiralama varsa devral (rezervasyon CONVERTED; geri sayım yok).
            val preparing = rentalRepository.findPreparingRental().getOrNull()
            if (preparing != null) {
                resumePreparing(preparing)
                return@launch
            }
            // 2) Normal mod: aktif rezervasyon → geri sayım + yerel çekim.
            reservationRepository.getActiveReservation()
                .onSuccess { reservation ->
                    startVehicleId = reservation.vehicleId
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            reservationId = reservation.reservationId,
                            rentalId = null,
                            vehicleTitle = reservation.vehicleTitle,
                            vehiclePlate = reservation.vehiclePlate,
                        )
                    }
                    startCountdown(reservation.remainingSeconds)
                }
                .onFailure { e ->
                    val error = e.toAppError()
                    if (error is AppError.Api && error.code == 404) {
                        // Aktif rezervasyon yok (süre doldu / iptal edildi) → bilgilendir, geri.
                        _uiState.update { it.copy(isLoading = false, reservationExpired = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loadError = error.toUserMessage(ErrorContext.RESERVATION_LOAD),
                            )
                        }
                    }
                }
        }
    }

    /**
     * Askıda PREPARING kiralamayı devralır (kurtarma modu): kimlik + araç başlığı state'e alınır, foto
     * durumu (yüklü yönler) GET /rentals/{id}/photos ile geri yüklenir. Foto durumu alınamazsa akış yine
     * sürdürülür (eksik yönler yerelde tekrar çekilip "Başlat"ta yüklenir). Bu modda geri sayım yoktur.
     */
    private suspend fun resumePreparing(preparing: ResumableRentalUi) {
        startVehicleId = preparing.vehicleId
        _uiState.update {
            it.copy(
                rentalId = preparing.rentalId,
                reservationId = null,
                reservationRemaining = null,
                vehicleTitle = preparing.vehicleTitle,
                vehiclePlate = preparing.vehiclePlate,
            )
        }
        rentalRepository.getPhotos(preparing.rentalId)
            .onSuccess { photos ->
                _uiState.update {
                    it.copy(isLoading = false, uploadedSides = photos.uploadedSides.toSides())
                }
            }
            .onFailure {
                _uiState.update { it.copy(isLoading = false) }
            }
    }

    /** Kalan süreyi 1 sn'lik yerel sayaçla azaltır; 0'a inince süre dolar (araç sunucuda boşa çıkar). */
    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            var remaining = seconds.coerceAtLeast(0)
            _uiState.update { it.copy(reservationRemaining = remaining) }
            while (remaining > 0) {
                delay(1000)
                remaining -= 1
                _uiState.update { it.copy(reservationRemaining = remaining) }
            }
            _uiState.update { it.copy(reservationRemaining = null, reservationExpired = true) }
        }
    }

    /** Çekilen kareyi yerelde saklar; yükleme "Başlat"a ertelenir (rezervasyon o ana dek aktif kalır). */
    private fun capturePhoto(side: PhotoSide, path: String) {
        _uiState.update {
            it.copy(capturedPaths = it.capturedPaths + (side to path), errorMessage = null)
        }
    }

    /**
     * "Kiralamayı Başlat" zinciri: (1) kiralama yoksa POST /rentals ile açar (rezervasyon CONVERTED),
     * (2) henüz yüklenmemiş kareleri sırayla POST /rentals/{id}/photos ile yükler, (3) POST
     * /rentals/{id}/start çağırır. Herhangi bir adımda hata olursa kimlik/ilerleme korunur ve kullanıcı
     * tekrar basınca kaldığı yerden sürer (yeniden create edilmez; yalnız eksik yönler yüklenir).
     */
    private fun startRental() {
        if (!_uiState.value.canStart) return
        countdownJob?.cancel() // zincir başladı; süre artık işlemez.
        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            // 1) Kiralamayı aç (yoksa). Rezervasyon burada CONVERTED olur.
            val rentalId = _uiState.value.rentalId ?: run {
                val created = rentalRepository.createRental(startVehicleId, plan).getOrElse { e ->
                    fail(e, ErrorContext.RENTAL_CREATE)
                    return@launch
                }
                _uiState.update {
                    it.copy(rentalId = created.id, reservationId = null, reservationRemaining = null)
                }
                created.id
            }

            // 2) Yüklenmemiş yönleri sırayla yükle.
            val pending = _uiState.value.capturedPaths
                .filterKeys { it !in _uiState.value.uploadedSides }
            for ((side, path) in pending) {
                _uiState.update { it.copy(uploadingSide = side) }
                val photos = rentalRepository.uploadPhoto(rentalId, side.apiValue, File(path))
                    .getOrElse { e ->
                        fail(e, ErrorContext.RENTAL_PHOTO_UPLOAD)
                        return@launch
                    }
                _uiState.update {
                    it.copy(uploadingSide = null, uploadedSides = photos.uploadedSides.toSides())
                }
            }

            // 3) Yolculuğu başlat (ACTIVE).
            rentalRepository.startRental(rentalId)
                .onSuccess { _uiState.update { it.copy(isStarting = false, started = true) } }
                .onFailure { e -> fail(e, ErrorContext.RIDE_START) }
        }
    }

    /**
     * "Rezervasyonu İptal Et": kiralama henüz oluşmadıysa aktif rezervasyonu DELETE /reservations/{id}
     * ile iptal eder; "Başlat" bir kiralama açtıysa (kurtarma / yarım zincir) DELETE /rentals/{id} ile
     * temizler. İkisi de aracı anında AVAILABLE yapar. İptal edilecek bir şey yoksa doğrudan çıkılır.
     */
    private fun cancel() {
        if (!_uiState.value.canCancel) return
        countdownJob?.cancel()
        _uiState.update { it.copy(isCancelling = true, errorMessage = null) }
        viewModelScope.launch {
            val rentalId = _uiState.value.rentalId
            val reservationId = _uiState.value.reservationId
            val result = when {
                rentalId != null -> rentalRepository.cancelRental(rentalId)
                reservationId != null -> reservationRepository.cancelReservation(reservationId)
                else -> Result.success(Unit)
            }
            result
                .onSuccess { _uiState.update { it.copy(isCancelling = false, cancelled = true) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isCancelling = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.RESERVATION_CANCEL),
                        )
                    }
                }
        }
    }

    /** Zincir hatası: spinner'ları kapatır, mesajı yazar; kimlik/ilerleme korunur (tekrar denenebilir). */
    private fun fail(error: Throwable, context: ErrorContext) {
        _uiState.update {
            it.copy(
                isStarting = false,
                uploadingSide = null,
                errorMessage = error.toAppError().toUserMessage(context),
            )
        }
    }

    /** API yön kodlarını ([RentalPhotosUi.uploadedSides]) tanınan [PhotoSide] kümesine çevirir. */
    private fun List<String>.toSides(): Set<PhotoSide> = mapNotNull { PhotoSide.fromApi(it) }.toSet()
}
