package com.turkcell.rencar.ui.map

import com.turkcell.rencar.data.model.GeoPoint
import com.turkcell.rencar.data.model.VehicleUi

/**
 * 04·Harita — saf UI durumu (§4.2).
 * Kullanıcının anlık konumu, konum izni ve haritada gösterilecek müsait araçları tutar.
 * Kamera/harita motoru gibi imperatif görsel işler
 * [com.turkcell.rencar.ui.map.RencarMapController] üzerinden Screen katmanında yürütülür.
 * Araç verisi VehicleRepository'den (data + repository, decisions.md) intent ile yüklenir.
 */
data class MapUiState(
    /** Anlık kullanıcı konumu; harita üzerindeki mavi noktayı besler (null → nokta gizli). */
    val myLocation: GeoPoint? = null,
    /** Konum izni verildi mi. Konum güncellemeleri yalnızca bu true iken başlar. */
    val hasLocationPermission: Boolean = false,
    /** Kullanıcı izni açıkça reddetti mi (sistem diyaloğunda). */
    val permissionDenied: Boolean = false,
    /** İlk konum geldiğinde tek seferlik kamera zoom'unun yapıldığını işaretler. */
    val hasCenteredOnUser: Boolean = false,
    /** GET /vehicles'tan gelen müsait araçlar; harita üzerinde fiyat balonu olarak çizilir. */
    val vehicles: List<VehicleUi> = emptyList(),
    /** Araç listesi yükleniyor mu (tekrarlı istekleri engellemek için de kullanılır). */
    val isLoadingVehicles: Boolean = false,
    /** Araç yüklemesi başarısızsa kullanıcıya gösterilecek mesaj (yoksa null). */
    val vehiclesError: String? = null,
    /** Haritada bir araca dokunulunca seçilen aracın id'si; detay alt sayfasını (bottom sheet) açar (null → kapalı). */
    val selectedVehicleId: String? = null,
    /**
     * Seçili fiyat segmenti filtresi (alt karttaki çip satırı). null → "Tümü" (filtre yok).
     * Geçerli değerler: ECONOMY | COMFORT | SUV (openapi segment enum'u). GET /vehicles'a
     * `segment` query'si olarak iletilir; değişince liste yeniden çekilir.
     */
    val selectedSegment: String? = null,
    /** Alt karttaki "Yakınında N araç" sayısı — yalnızca müsait (AVAILABLE) araçlar sayılır. */
    val availableCount: Int = 0,
    /**
     * Kullanıcıya en yakın MÜSAİT araç (konum varsa mesafeye göre, yoksa listedeki ilki).
     * "En Yakın Aracı Bul" butonu bu araca kamerayı taşır ve detayını açar.
     */
    val nearestVehicle: VehicleUi? = null,
    /** En yakın araca düz mesafe (metre); konum yoksa null. Alt kart altyazısındaki ~dk buradan tahmin edilir. */
    val nearestDistanceMeters: Float? = null,
    /**
     * Cihaz Geocoder'ından çözülen mahalle/semt adı (ör. "Kadıköy"). API DIŞI bir cihaz
     * özelliğidir (decisions.md); alt kart altyazısında gösterilir, yoksa gizlenir.
     */
    val localityName: String? = null,
    /**
     * Alt bilgi kartı açık mı (genişletilmiş). true → tam kart (çipler + "En Yakın Aracı Bul");
     * false → yalnız başlık satırı (kullanıcı tutamaca dokununca aç/kapa yapar).
     */
    val bottomCardExpanded: Boolean = true,
    /** AI tarafından önerilen araçların ID'leri; haritada vurgulanır. */
    val recommendedVehicleIds: Set<String> = emptySet(),
    /** AI öneri diyaloğu gösteriliyor mu. */
    val showAiDialog: Boolean = false,
)

/**
 * Kullanıcı/framework aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface MapIntent {
    /** Sistem izin diyaloğunun sonucu (launcher'dan). */
    data class PermissionResult(val granted: Boolean) : MapIntent

    /** Fused location'dan gelen taze/güncel konum. */
    data class LocationChanged(val location: GeoPoint) : MapIntent

    /** İlk konuma tek seferlik kamera zoom'u tamamlandı (tekrar zoom'u önler). */
    data object CenteredOnUser : MapIntent

    /** Sağ alt FAB: konumu yenile ve kamerayı tekrar ortala (Screen'de ele alınır). */
    data object RecenterClicked : MapIntent

    /** Müsait araçları GET /vehicles ile yükle (ekran açılışında tetiklenir). */
    data object LoadVehicles : MapIntent

    /**
     * Segment çipine dokunuldu. [segment] null → "Tümü"; aksi halde ECONOMY/COMFORT/SUV.
     * Seçim state'e yazılır ve araç listesi bu segmentle yeniden yüklenir.
     */
    data class SegmentSelected(val segment: String?) : MapIntent

    /** Haritada bir araca dokunuldu; detay alt sayfasını açmak için seçili id ayarlanır. */
    data class VehicleClicked(val id: String) : MapIntent

    /** Detay alt sayfası kapatıldı; seçim temizlenir. */
    data object VehicleDismissed : MapIntent

    /** Sağ alt "+" butonu: kamerayı bir kademe yakınlaştır (Screen'de ele alınır). */
    data object ZoomIn : MapIntent

    /** Sağ alt "-" butonu: kamerayı bir kademe uzaklaştır (Screen'de ele alınır). */
    data object ZoomOut : MapIntent

    /** "En Yakın Aracı Bul": en yakın müsait araca kamerayı taşı + detayını aç (Screen'de ele alınır). */
    data object FindNearest : MapIntent

    /** Alt bilgi kartının tutamacına dokunuldu; kart açık/kapalı durumu terslenir. */
    data object ToggleBottomCard : MapIntent

    /** Cihaz Geocoder'ı konumu bir mahalle/semt adına çevirdi (null → çözülemedi). */
    data class LocalityResolved(val name: String?) : MapIntent

    /** AI öneri butonuna tıklandı; diyaloğu açar. */
    data object AiClicked : MapIntent

    /** AI diyaloğu kapatıldı. */
    data object AiDismissed : MapIntent

    /** AI önerileri temizlendi (filtre kalkar). */
    data object ClearAiRecommendations : MapIntent

    /** AI önerileri geldi; haritada vurgulanacak ID'leri ayarlar. */
    data class SetAiRecommendations(val ids: Set<String>) : MapIntent
}
