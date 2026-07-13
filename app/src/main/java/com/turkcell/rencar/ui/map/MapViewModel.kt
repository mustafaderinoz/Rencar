package com.turkcell.rencar.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

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
                _uiState.update { it.copy(myLocation = intent.location) }

            MapIntent.CenteredOnUser ->
                _uiState.update { it.copy(hasCenteredOnUser = true) }

            // Kamera yeniden ortalama saf Screen mekaniğidir (controller + fused location);
            // burada durum değişmez, ekran (Screen) katmanında ele alınır.
            MapIntent.RecenterClicked -> Unit

            MapIntent.LoadVehicles -> loadVehicles()

            is MapIntent.VehicleClicked ->
                _uiState.update { it.copy(selectedVehicleId = intent.id) }

            MapIntent.VehicleDismissed ->
                _uiState.update { it.copy(selectedVehicleId = null) }
        }
    }

    /** GET /vehicles: müsait araçları yükler; süregelen bir istek varsa yeni istek başlatılmaz. */
    private fun loadVehicles() {
        if (_uiState.value.isLoadingVehicles) return

        _uiState.update { it.copy(isLoadingVehicles = true, vehiclesError = null) }
        viewModelScope.launch {
            vehicleRepository.getAvailableVehicles()
                .onSuccess { vehicles ->
                    _uiState.update { it.copy(isLoadingVehicles = false, vehicles = vehicles) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoadingVehicles = false, vehiclesError = e.toMessage()) }
                }
        }
    }

    private fun Throwable.toMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Araçları görmek için ehliyet onayınız gerekli."
            else -> "Araçlar yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
