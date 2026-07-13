package com.turkcell.rencar.ui.map

import com.turkcell.rencar.data.remote.dto.VehicleResponse
import org.maplibre.android.geometry.LatLng

/**
 * 04·Harita — saf UI durumu (§4.2).
 * Kullanıcının anlık konumu, konum izni ve haritada gösterilecek müsait araçları tutar.
 * Kamera/harita motoru gibi imperatif görsel işler
 * [com.turkcell.rencar.ui.map.RencarMapController] üzerinden Screen katmanında yürütülür.
 * Araç verisi VehicleRepository'den (data + repository, decisions.md) intent ile yüklenir.
 */
data class MapUiState(
    /** Anlık kullanıcı konumu; harita üzerindeki mavi noktayı besler (null → nokta gizli). */
    val myLocation: LatLng? = null,
    /** Konum izni verildi mi. Konum güncellemeleri yalnızca bu true iken başlar. */
    val hasLocationPermission: Boolean = false,
    /** Kullanıcı izni açıkça reddetti mi (sistem diyaloğunda). */
    val permissionDenied: Boolean = false,
    /** İlk konum geldiğinde tek seferlik kamera zoom'unun yapıldığını işaretler. */
    val hasCenteredOnUser: Boolean = false,
    /** GET /vehicles'tan gelen müsait araçlar; harita üzerinde fiyat balonu olarak çizilir. */
    val vehicles: List<VehicleResponse> = emptyList(),
    /** Araç listesi yükleniyor mu (tekrarlı istekleri engellemek için de kullanılır). */
    val isLoadingVehicles: Boolean = false,
    /** Araç yüklemesi başarısızsa kullanıcıya gösterilecek mesaj (yoksa null). */
    val vehiclesError: String? = null,
    /** Haritada bir araca dokunulunca seçilen aracın id'si; detay alt sayfasını (bottom sheet) açar (null → kapalı). */
    val selectedVehicleId: String? = null,
)

/**
 * Kullanıcı/framework aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface MapIntent {
    /** Sistem izin diyaloğunun sonucu (launcher'dan). */
    data class PermissionResult(val granted: Boolean) : MapIntent

    /** Fused location'dan gelen taze/güncel konum. */
    data class LocationChanged(val location: LatLng) : MapIntent

    /** İlk konuma tek seferlik kamera zoom'u tamamlandı (tekrar zoom'u önler). */
    data object CenteredOnUser : MapIntent

    /** Sağ alt FAB: konumu yenile ve kamerayı tekrar ortala (Screen'de ele alınır). */
    data object RecenterClicked : MapIntent

    /** Müsait araçları GET /vehicles ile yükle (ekran açılışında tetiklenir). */
    data object LoadVehicles : MapIntent

    /** Haritada bir araca dokunuldu; detay alt sayfasını açmak için seçili id ayarlanır. */
    data class VehicleClicked(val id: String) : MapIntent

    /** Detay alt sayfası kapatıldı; seçim temizlenir. */
    data object VehicleDismissed : MapIntent
}
