package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.remote.api.VehicleApi
import com.turkcell.rencar.data.remote.dto.VehicleResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Müsait araç listeleme iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → VehicleApi. Hata yönetimi Result ile çağırana taşınır
 * (mesaj eşlemesi ViewModel'de). Ayrı domain/UseCase katmanı eklenmez (AGENTS §4.6).
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleApi: VehicleApi,
) {
    /**
     * Müsait (AVAILABLE) araçları getirir. [type] verilirse ilgili araç tipiyle filtreler
     * (SEDAN/SUV/HATCHBACK/STATION/MINIVAN); null ise tüm müsait araçlar döner.
     */
    suspend fun getAvailableVehicles(type: String? = null): Result<List<VehicleResponse>> =
        runCatching { vehicleApi.list(type = type) }

    /**
     * Tek aracın detayını getirir (araç detay ekranı). Görünmeyen/olmayan araç için API 404
     * döndürür; hata Result olarak çağırana (ViewModel) taşınır.
     */
    suspend fun getVehicle(id: String): Result<VehicleResponse> =
        runCatching { vehicleApi.getOne(id) }
}
