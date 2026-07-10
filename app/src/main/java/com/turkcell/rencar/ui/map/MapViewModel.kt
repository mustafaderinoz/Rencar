package com.turkcell.rencar.ui.map

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Harita ekranının tek durum kaynağı (§4.4). Konum ve izin bilgisini tutar; framework
 * mekaniği (izin launcher'ı, FusedLocation, kamera) Screen'de kalır ve buraya intent olarak
 * yansır. Konum için ayrı Repository/UseCase eklenmez (§4.6).
 */
@HiltViewModel
class MapViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    fun onIntent(intent: MapIntent) {
        when (intent) {
            is MapIntent.PermissionResult ->
                _uiState.update {
                    it.copy(
                        hasLocationPermission = intent.granted,
                        permissionDenied = !intent.granted,
                    )
                }

            is MapIntent.LocationChanged ->
                _uiState.update { it.copy(myLocation = intent.location) }

            MapIntent.CenteredOnUser ->
                _uiState.update { it.copy(hasCenteredOnUser = true) }

            // Kamera yeniden ortalama saf Screen mekaniğidir (controller + fused location);
            // burada durum değişmez, ekran (Screen) katmanında ele alınır.
            MapIntent.RecenterClicked -> Unit
        }
    }
}
