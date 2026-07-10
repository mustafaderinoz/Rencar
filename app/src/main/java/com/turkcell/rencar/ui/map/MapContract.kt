package com.turkcell.rencar.ui.map

import org.maplibre.android.geometry.LatLng

/**
 * 04·Harita — saf UI durumu (§4.2).
 * Kullanıcının anlık konumu ve konum izni durumunu tutar. Kamera/harita motoru gibi
 * imperatif görsel işler [com.turkcell.rencar.ui.map.RencarMapController] üzerinden
 * Screen katmanında yürütülür (§4.6: Effect/UseCase/Repository eklenmez).
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
}
