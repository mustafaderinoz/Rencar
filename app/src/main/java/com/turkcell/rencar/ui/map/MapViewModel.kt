package com.turkcell.rencar.ui.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.data.repository.VehicleRepository
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Harita ekranının tek durum kaynağı (§4.4). Konum/izin bilgisini ve haritada gösterilecek
 * müsait araç listesini tutar. Konum framework mekaniği (izin launcher'ı, FusedLocation,
 * kamera) Screen'de kalır ve buraya intent olarak yansır; araç verisi [VehicleRepository]
 * üzerinden yüklenir (data + repository, decisions.md).
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

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
                _uiState.update { it.copy(myLocation = intent.location).withDerived() }

            MapIntent.CenteredOnUser ->
                _uiState.update { it.copy(hasCenteredOnUser = true) }

            // Kamera yeniden ortalama saf Screen mekaniğidir (controller + fused location);
            // burada durum değişmez, ekran (Screen) katmanında ele alınır.
            MapIntent.RecenterClicked -> Unit

            MapIntent.LoadVehicles -> loadVehicles()

            is MapIntent.SegmentSelected -> {
                // Aynı segment tekrar seçilirse yeniden yükleme yapma (gereksiz istek).
                if (intent.segment == _uiState.value.selectedSegment) return
                _uiState.update { it.copy(selectedSegment = intent.segment) }
                loadVehicles()
            }

            is MapIntent.VehicleClicked ->
                _uiState.update { it.copy(selectedVehicleId = intent.id) }

            MapIntent.VehicleDismissed ->
                _uiState.update { it.copy(selectedVehicleId = null) }

            // Zoom ve "En Yakın Aracı Bul" saf Screen mekaniğidir (controller); durum değişmez.
            MapIntent.ZoomIn, MapIntent.ZoomOut, MapIntent.FindNearest -> Unit

            // Navigasyon Screen katmanında ele alınır (§4.5); seçim VehicleDismissed ile temizlenir.
            is MapIntent.ReserveClicked -> Unit

            MapIntent.ToggleBottomCard ->
                _uiState.update { it.copy(bottomCardExpanded = !it.bottomCardExpanded) }

            is MapIntent.LocalityResolved ->
                _uiState.update { it.copy(localityName = intent.name) }

            MapIntent.AiClicked ->
                _uiState.update { it.copy(showAiDialog = true) }

            MapIntent.AiDismissed ->
                _uiState.update { it.copy(showAiDialog = false) }

            MapIntent.ClearAiRecommendations ->
                _uiState.update { it.copy(recommendedVehicleIds = emptySet()).withDerived() }

            is MapIntent.SetAiRecommendations ->
                _uiState.update { state ->
                    val recommendedVehicles = state.vehicles.filter { it.id in intent.ids }
                    val distinctSegments = recommendedVehicles.mapNotNull { it.segment }.distinct()
                    val inferredSegment = if (distinctSegments.size == 1) distinctSegments.first() else null

                    state.copy(
                        recommendedVehicleIds = intent.ids,
                        selectedSegment = inferredSegment ?: state.selectedSegment,
                        bottomCardExpanded = true // Sonuçlar gelince kartı aç ki sayı değişimi görülsün
                    ).withDerived()
                }
        }
    }

    /**
     * GET /vehicles: seçili segmentteki araçları yükler (gri "Kullanımda" balonları için
     * includeBusy=true). Süregelen bir istek varsa yeni istek başlatılmaz; segment değişiminde
     * yeniden çağrılır. Başarıda "yakında/en yakın" türetilmiş alanlar da güncellenir.
     */
    private fun loadVehicles() {
        if (_uiState.value.isLoadingVehicles) return

        _uiState.update { it.copy(isLoadingVehicles = true, vehiclesError = null) }
        viewModelScope.launch {
            vehicleRepository.getAvailableVehicles(
                segment = _uiState.value.selectedSegment,
                includeBusy = true,
            )
                .onSuccess { vehicles ->
                    _uiState.update { it.copy(isLoadingVehicles = false, vehicles = vehicles).withDerived() }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingVehicles = false,
                            vehiclesError = e.toAppError().toUserMessage(ErrorContext.MAP),
                        )
                    }
                }
        }
    }

    /**
     * Araç listesi veya konum değiştiğinde alt kartı besleyen türetilmiş alanları yeniden hesaplar:
     * müsait araç sayısı, en yakın müsait araç ve ona olan düz mesafe. Konum yoksa "en yakın"
     * listedeki ilk müsait araçtır (mesafe null).
     */
    private fun MapUiState.withDerived(): MapUiState {
        val available = vehicles.filter { v ->
            val isStatusAvailable = VehicleMarkers.isAvailable(v.status)
            // Eğer AI önerileri varsa, sadece o listedekileri "müsait" ve "gösterilebilir" say
            if (recommendedVehicleIds.isNotEmpty()) {
                isStatusAvailable && v.id in recommendedVehicleIds
            } else {
                isStatusAvailable
            }
        }
        val loc = myLocation
        val nearest: VehicleUi?
        val distance: Float?
        if (loc == null) {
            nearest = available.firstOrNull()
            distance = null
        } else {
            nearest = available.minByOrNull { distanceMeters(loc.latitude, loc.longitude, it) }
            distance = nearest?.let { distanceMeters(loc.latitude, loc.longitude, it) }
        }
        return copy(
            availableCount = available.size,
            nearestVehicle = nearest,
            nearestDistanceMeters = distance,
        )
    }

    /** Kullanıcı konumu ↔ araç konumu arası düz mesafe (metre). */
    private fun distanceMeters(userLat: Double, userLng: Double, vehicle: VehicleUi): Float {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, vehicle.latitude, vehicle.longitude, results)
        return results[0]
    }
}
