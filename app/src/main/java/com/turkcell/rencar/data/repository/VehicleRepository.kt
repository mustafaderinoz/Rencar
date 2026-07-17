package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.data.remote.api.VehicleApi
import com.turkcell.rencar.data.remote.dto.VehicleResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Müsait araç listeleme iş akışı (karar: decisions.md → data + repository + ayrı mapper katmanı).
 * ViewModel → Repository → VehicleApi. API yanıtı (DTO) UI'a doğrudan verilmez; repository ayrı
 * mapper katmanı ([com.turkcell.rencar.data.mapper.toUi]) ile [VehicleUi]'ye çevirir. Hata yönetimi
 * Result ile çağırana taşınır (mesaj eşlemesi ViewModel'de).
 */
@Singleton
class VehicleRepository @Inject constructor(
    private val vehicleApi: VehicleApi,
) {
    /**
     * Müsait (AVAILABLE) araçları getirir. [type] verilirse araç tipiyle
     * (SEDAN/SUV/HATCHBACK/STATION/MINIVAN), [segment] verilirse fiyat segmentiyle
     * (ECONOMY/COMFORT/SUV — haritadaki Tümü/Ekonomik/Konfor/SUV çipleri) filtreler;
     * null gönderilen parametreler Retrofit tarafından atlanır (tümü döner).
     *
     * [includeBusy] true ise RENTED/RESERVED araçlar da döner (haritada gri "Kullanımda"
     * marker'ları için); istemci `status` alanına bakar. Varsayılan yalnızca AVAILABLE.
     */
    suspend fun getAvailableVehicles(
        type: String? = null,
        segment: String? = null,
        includeBusy: Boolean = false,
    ): Result<List<VehicleUi>> =
        runCatching {
            // GET /vehicles SAYFALIDIR: page/limit gönderilmezse sunucu yalnızca varsayılan ilk
            // sayfayı (20 araç, en son eklenenler) döner → başka şehirlerdeki araçlar hiç gelmez.
            // API'de konum/şehir filtresi olmadığından (§2.2), tüm sayfalar limit=100 (API maks.)
            // ile dolaşılıp birleştirilir; harita zaten görünürdeki araçları eler.
            val all = mutableListOf<VehicleResponse>()
            var page = 1
            while (page <= MAX_PAGES) {
                val batch = vehicleApi.list(
                    type = type,
                    segment = segment,
                    includeBusy = if (includeBusy) "true" else null,
                    page = page,
                    limit = PAGE_SIZE,
                )
                all += batch
                if (batch.size < PAGE_SIZE) break // eksik dolu sayfa = son sayfa
                page++
            }
            all.toUi()
        }

    /**
     * Tek aracın detayını getirir (araç detay ekranı). Görünmeyen/olmayan araç için API 404
     * döndürür; hata Result olarak çağırana (ViewModel) taşınır.
     */
    suspend fun getVehicle(id: String): Result<VehicleUi> =
        runCatching { vehicleApi.getOne(id).toUi() }

    private companion object {
        /** Sayfa başına araç (openapi: limit maks. 100). */
        const val PAGE_SIZE = 100

        /** Güvenlik sınırı: olası hatalı sunucu davranışında sonsuz döngüyü engeller (100×20 = 2000 araç). */
        const val MAX_PAGES = 20
    }
}
