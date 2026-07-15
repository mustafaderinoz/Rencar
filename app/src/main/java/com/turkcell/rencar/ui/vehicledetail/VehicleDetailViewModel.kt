package com.turkcell.rencar.ui.vehicledetail

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.VehicleUi
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
 * Araç Detay ekranının tek durum kaynağı (§4.4). GET /vehicles/{id} ile aracı çeker (statik değil)
 * ve kullanıcı konumu verildiyse uzaklığı gerçek hesaplar. Ayrı domain/UseCase katmanı yoktur
 * (data + repository, decisions.md); framework konum mekaniği Screen/Map tarafında kalır, buraya
 * yalnızca lat/long değeri intent olarak gelir.
 */
@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleDetailUiState())
    val uiState: StateFlow<VehicleDetailUiState> = _uiState.asStateFlow()

    // Retry için son yükleme parametreleri saklanır.
    private var lastLoad: VehicleDetailIntent.Load? = null

    fun onIntent(intent: VehicleDetailIntent) {
        when (intent) {
            is VehicleDetailIntent.Load -> load(intent)
            VehicleDetailIntent.Retry -> lastLoad?.let { load(it) }
            // Rezervasyon ucu (POST /reservations) bu iş kapsamında bağlanmadı (§4.6):
            // buton yalnızca durum (status) ile aktif/pasif çizilir; navigasyonu Screen katmanı yapar.
            VehicleDetailIntent.ReserveClicked -> Unit
        }
    }

    /** GET /vehicles/{id}: aracı yükler; başarıda uzaklığı hesaplar. Aynı id tekrar yüklenmez (yükleniyorken). */
    private fun load(intent: VehicleDetailIntent.Load) {
        lastLoad = intent
        val alreadyLoaded = _uiState.value.vehicle?.id == intent.vehicleId
        if (_uiState.value.isLoading && alreadyLoaded) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            vehicleRepository.getVehicle(intent.vehicleId)
                .onSuccess { vehicle ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            vehicle = vehicle,
                            distanceMeters = distanceTo(vehicle, intent.userLatitude, intent.userLongitude),
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toMessage()) }
                }
        }
    }

    /** Kullanıcı konumu ↔ araç konumu arası düz mesafeyi (metre) hesaplar; konum yoksa null. */
    private fun distanceTo(vehicle: VehicleUi, userLat: Double?, userLng: Double?): Float? {
        if (userLat == null || userLng == null) return null
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, vehicle.latitude, vehicle.longitude, results)
        return results[0]
    }

    private fun Throwable.toMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Araç detayını görmek için ehliyet onayınız gerekli."
            404 -> "Bu araç şu anda müsait değil."
            else -> "Araç yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
