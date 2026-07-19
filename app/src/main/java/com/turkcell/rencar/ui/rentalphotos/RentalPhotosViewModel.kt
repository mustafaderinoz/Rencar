package com.turkcell.rencar.ui.rentalphotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.ResumableRentalUi
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        resumeOrCreate()
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
            RentalPhotosIntent.Retry -> resumeOrCreate()
            is RentalPhotosIntent.PhotoCaptured -> uploadPhoto(intent.side, intent.path)
            RentalPhotosIntent.StartClicked -> startRental()
            // Navigasyon Screen katmanında ele alınır (§4.6).
            RentalPhotosIntent.BackClicked -> Unit

            // Ekran geçişi yaptı → bayrağı tüket (tekrar geçişi önler).
            RentalPhotosIntent.StartedHandled -> _uiState.update { it.copy(started = false) }
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
                    _uiState.update {
                        it.copy(
                            isStarting = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.RIDE_START),
                        )
                    }
                }
        }
    }

    /**
     * Açılış/tekrar-dene: kullanıcının açık (PREPARING) kiralaması varsa yeni açmak yerine foto
     * akışını DEVRALIR (GET /rentals/{id}/photos ile yüklü yönler geri yüklenir); yoksa yeni PREPARING
     * kiralama açar (POST /rentals). Uygulama beklenmedik kapanıp açıldığında yarım kalan akış buradan
     * sürer (openapi resume sözleşmesi). Ekrandan bilerek geri çıkış hâlâ [onCleared] ile iptal eder.
     */
    private fun resumeOrCreate() {
        _uiState.update { it.copy(isCreating = true, createError = null) }
        viewModelScope.launch {
            val preparing = rentalRepository.findPreparingRental().getOrNull()
            if (preparing != null) {
                resumeRental(preparing)
            } else {
                createRental()
            }
        }
    }

    /**
     * Açık PREPARING kiralamayı devralır: kimlik + araç başlığı state'e alınır, foto durumu (yüklü
     * yönler/sayaç) GET /rentals/{id}/photos ile geri yüklenir. Foto durumu alınamazsa akış yine
     * sürdürülür (eksik yönler tekrar çekilebilir). Yerel önizleme yolları olmadığından devralınan
     * yönler yalnız yeşil rozetle gösterilir (SideCell [RentalPhotosUiState.uploadedSides]'a bakar).
     */
    private suspend fun resumeRental(preparing: ResumableRentalUi) {
        _uiState.update {
            it.copy(
                rentalId = preparing.rentalId,
                vehicleTitle = preparing.vehicleTitle,
                vehiclePlate = preparing.vehiclePlate,
            )
        }
        rentalRepository.getPhotos(preparing.rentalId)
            .onSuccess { photosState ->
                _uiState.update { it.applyPhotosState(photosState).copy(isCreating = false) }
            }
            .onFailure {
                _uiState.update { it.copy(isCreating = false) }
            }
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
                    _uiState.update {
                        it.copy(
                            isCreating = false,
                            createError = e.toAppError().toUserMessage(ErrorContext.RENTAL_CREATE),
                        )
                    }
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
                            errorMessage = e.toAppError()
                                .toUserMessage(ErrorContext.RENTAL_PHOTO_UPLOAD),
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
}
