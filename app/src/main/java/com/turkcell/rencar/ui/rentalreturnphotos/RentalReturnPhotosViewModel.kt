package com.turkcell.rencar.ui.rentalreturnphotos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Araç teslim durumu (kiralama sonrası fotoğraf) ekranının tek durum kaynağı (§4.4).
 *
 * Aktif Yolculuk'tan iletilen rentalId + araç özeti path argümanlarını [SavedStateHandle] ile okur.
 * Kiralama bu ekran açılmadan ÖNCE bitirilmiştir (finish); burada ağ çağrısı yapılmaz — teslim
 * fotoğrafı ucu backend'de yoktur (bkz. [RentalReturnPhotosUiState] MOCK notu, §2.2). Android
 * API'lerine (kamera/FileProvider) dokunmaz; ekran çekim yapıp yolu intent ile iletir.
 */
@HiltViewModel
class RentalReturnPhotosViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RentalReturnPhotosUiState(
            rentalId = savedStateHandle
                .get<String>(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_RENTAL_ID).orEmpty(),
            vehicleTitle = savedStateHandle
                .get<String>(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_VEHICLE_TITLE).orEmpty(),
            vehiclePlate = savedStateHandle
                .get<String>(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_VEHICLE_PLATE).orEmpty(),
        ),
    )
    val uiState: StateFlow<RentalReturnPhotosUiState> = _uiState.asStateFlow()

    fun onIntent(intent: RentalReturnPhotosIntent) {
        when (intent) {
            is RentalReturnPhotosIntent.PhotoCaptured ->
                _uiState.update { it.copy(capturedPaths = it.capturedPaths + (intent.side to intent.path)) }

            // Navigasyon Screen katmanında ele alınır (§4.6).
            RentalReturnPhotosIntent.ContinueClicked -> Unit
            RentalReturnPhotosIntent.BackClicked -> Unit
        }
    }
}
