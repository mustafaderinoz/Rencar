package com.turkcell.rencar.ui.rentalphotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Araç durumu (kiralama öncesi fotoğraf) ekranının tek durum kaynağı (§4.4).
 *
 * Rezervasyondan iletilen vehicleId + plan path argümanlarını [SavedStateHandle] ile okur;
 * açılışta POST /rentals ile kiralamayı PREPARING oluşturur. Her yön çekildiğinde
 * POST /rentals/{id}/photos çağrılır; sayaç/kalan yönler API cevabından güncellenir.
 * Android API'lerine (kamera/FileProvider) dokunmaz; ekran çekim yapıp yolu intent ile iletir.
 * Ayrı domain/UseCase katmanı yoktur (decisions.md).
 */
@HiltViewModel
class RentalPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rentalRepository: RentalRepository,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val vehicleId: String =
        savedStateHandle.get<String>(RencarDestinations.RENTAL_PHOTOS_ARG_VEHICLE_ID).orEmpty()
    private val plan: String =
        savedStateHandle.get<String>(RencarDestinations.RENTAL_PHOTOS_ARG_PLAN).orEmpty()

    private val _uiState = MutableStateFlow(RentalPhotosUiState())
    val uiState: StateFlow<RentalPhotosUiState> = _uiState.asStateFlow()

    // Yolculuk başlatıldıysa (POST /rentals/{id}/start başarılı) true; ekrandan çıkarken kiralama
    // korunur. Aksi halde (geri/sistem geri) onCleared PREPARING kiralamayı iptal eder.
    private var startConfirmed = false

    init {
        createRental()
    }

    /**
     * Ekran kapanırken (geri/sistem geri) başlatılmamış PREPARING kiralamayı iptal eder.
     * viewModelScope geçişte iptal olacağı için istek [appScope] üzerinde yürütülür.
     */
    override fun onCleared() {
        val rentalId = _uiState.value.rentalId
        if (rentalId != null && !startConfirmed) {
            appScope.launch { rentalRepository.cancelRental(rentalId) }
        }
    }

    fun onIntent(intent: RentalPhotosIntent) {
        when (intent) {
            RentalPhotosIntent.Retry -> createRental()
            is RentalPhotosIntent.PhotoCaptured -> uploadPhoto(intent.side, intent.path)
            RentalPhotosIntent.StartClicked -> startRental()
        }
    }

    /** POST /rentals/{id}/start: yolculuğu ACTIVE yapar; başarıda geçiş bayrağı verilir. */
    private fun startRental() {
        val state = _uiState.value
        if (!state.canStart) return
        val rentalId = state.rentalId ?: return

        _uiState.update { it.copy(isStarting = true, errorMessage = null) }
        viewModelScope.launch {
            rentalRepository.startRental(rentalId)
                .onSuccess {
                    // Yolculuk ACTIVE: ekrandan çıkışta kiralama iptal EDİLMEZ.
                    startConfirmed = true
                    _uiState.update { it.copy(isStarting = false, started = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isStarting = false, errorMessage = e.toStartMessage()) }
                }
        }
    }

    /** Ekran, started bayrağını navigasyonda tüketince çağrılır (tekrar geçişi önler). */
    fun onStartedHandled() {
        _uiState.update { it.copy(started = false) }
    }

    /** POST /rentals: PREPARING kiralamayı açar; başlık için araç özetini state'e alır. */
    private fun createRental() {
        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            rentalRepository.createRental(vehicleId, plan)
                .onSuccess { rental ->
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            rentalId = rental.id,
                            vehicleTitle = rental.vehicleTitle,
                            vehiclePlate = rental.vehiclePlate,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isCreating = false, createError = e.toCreateMessage()) }
                }
        }
    }

    /** POST /rentals/{id}/photos: bir yönü yükler; sayaç/kalan yönler cevaptan güncellenir. */
    private fun uploadPhoto(side: PhotoSide, path: String) {
        val rentalId = _uiState.value.rentalId ?: return
        // Aynı anda tek yön yüklensin (kart spinner'ı tek); yükleme sürerken yeni çekim atlanır.
        if (_uiState.value.uploadingSide != null) return

        _uiState.update {
            it.copy(
                uploadingSide = side,
                capturedPaths = it.capturedPaths + (side to path),
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            rentalRepository.uploadPhoto(rentalId, side.apiValue, File(path))
                .onSuccess { photosState ->
                    _uiState.update { it.applyPhotosState(photosState).copy(uploadingSide = null) }
                }
                .onFailure { e ->
                    // Yükleme başarısız: yakalanan yolu geri al, hatayı göster (yeniden çekilebilir).
                    _uiState.update {
                        it.copy(
                            uploadingSide = null,
                            capturedPaths = it.capturedPaths - side,
                            errorMessage = e.toUploadMessage(),
                        )
                    }
                }
        }
    }

    /** POST cevabındaki güncel foto durumunu (yüklü yönler + sayaç + tamamlanma) uygular. */
    private fun RentalPhotosUiState.applyPhotosState(state: RentalPhotosUi): RentalPhotosUiState =
        copy(
            uploadedSides = state.uploadedSides.mapNotNull { PhotoSide.fromApi(it) }.toSet(),
            uploadedCount = state.uploadedCount,
            photosComplete = state.photosComplete,
        )

    private fun Throwable.toCreateMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Kiralama için ehliyet onayınız gerekli."
            404 -> "Araç bulunamadı."
            409 -> "Kiralama başlatılamadı: rezervasyonunuz bulunamadı veya araç müsait değil."
            else -> "Kiralama oluşturulamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toStartMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Bu kiralama size ait değil."
            404 -> "Kiralama bulunamadı."
            409 -> "Yolculuk başlatılamadı: fotoğraflar eksik veya yolculuk zaten başlamış."
            else -> "Yolculuk başlatılamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toUploadMessage(): String = when (this) {
        is HttpException -> when (code()) {
            400 -> "Fotoğraf geçersiz. Lütfen tekrar çekin."
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Bu kiralama size ait değil."
            404 -> "Kiralama bulunamadı."
            409 -> "Yolculuk zaten başlamış; fotoğraf eklenemiyor."
            413 -> "Fotoğraf çok büyük (maks. 5MB)."
            else -> "Fotoğraf yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
