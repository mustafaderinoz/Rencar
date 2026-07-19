package com.turkcell.rencar.ui.rentalphotos

/**
 * Araç durumu (kiralama öncesi fotoğraf) — saf UI durumu (§4.2).
 *
 * Rezervasyon "Tamamla" (Dakikalık/Saatlik plan) sonrası açılır. AKIŞ (decisions.md → "Rezervasyon →
 * Foto → Başlat"): ekran açılışında kiralama OLUŞTURULMAZ; bunun yerine aktif rezervasyonun 15 dk
 * geri sayımı gösterilir ve rezervasyon "Kiralamayı Başlat"a dek AKTİF kalır. Çekilen kareler yerelde
 * tutulur; yalnızca "Başlat" anında POST /rentals (rezervasyon CONVERTED olur) → 4 foto upload →
 * POST /rentals/{id}/start zinciri çalışır. Rezervasyon SADECE buradaki "Rezervasyonu İptal Et"
 * butonuyla iptal edilir (DELETE /reservations/{id}); geri çıkış rezervasyonu iptal etmez.
 *
 * Alanlar yalnızca UI durumunu tutar; navigasyon ekran katmanındadır (§4.5–4.6).
 */
data class RentalPhotosUiState(
    /** İlk yükleme (aktif rezervasyon / PREPARING kurtarma) sürüyor. */
    val isLoading: Boolean = true,
    /**
     * Aktif rezervasyonun kimliği (normal mod); "Rezervasyonu İptal Et" DELETE /reservations/{id}'i
     * bununla çağırır. Kiralama oluşturulunca (Başlat) null'a çekilir (rezervasyon CONVERTED olur).
     */
    val reservationId: String? = null,
    /**
     * Oluşturulan kiralamanın kimliği; yalnız "Başlat" zinciri kiralamayı açtıktan sonra veya PREPARING
     * kurtarma modunda doludur. Doluysa iptal DELETE /rentals/{id} ile temizlenir.
     */
    val rentalId: String? = null,
    /** Başlık: "Renault Clio" (araç özeti). */
    val vehicleTitle: String = "",
    /** Alt başlık: "34 RNC 022" (plaka). */
    val vehiclePlate: String = "",
    /**
     * 15 dk ücretsiz tutmanın kalan süresi (sn); null iken geri sayım gösterilmez (PREPARING kurtarma
     * modu ya da süre dolmuş). GET /reservations/active'ten alınıp 1 sn'lik yerel sayaçla azalır.
     */
    val reservationRemaining: Int? = null,
    /** Rezervasyon süresi bu ekrandayken doldu (araç sunucuda boşa çıktı) → bilgilendirme + geri. */
    val reservationExpired: Boolean = false,
    /** Yerelde çekilmiş yönlerin dosya yolları (yükleme "Başlat"a ertelenir). */
    val capturedPaths: Map<PhotoSide, String> = emptyMap(),
    /** Sunucuya yüklenmiş yönler (PREPARING kurtarma veya "Başlat" sırasında upload edilenler). */
    val uploadedSides: Set<PhotoSide> = emptySet(),
    /** "Başlat" zinciri (create→upload→start) sürüyor — buton spinner. */
    val isStarting: Boolean = false,
    /** "Başlat" sırasında o an yüklenen yön (kartta spinner); yoksa null. */
    val uploadingSide: PhotoSide? = null,
    /** Yolculuk başarıyla başladı (ACTIVE) → geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val started: Boolean = false,
    /** "Rezervasyonu İptal Et" sürüyor — buton spinner. */
    val isCancelling: Boolean = false,
    /** Rezervasyon/kiralama iptal edildi → Home'a dönüş sinyali (§4.6). */
    val cancelled: Boolean = false,
    /** Başlat/iptal/foto hatası (içerik altında gösterilir); yoksa null. */
    val errorMessage: String? = null,
    /** İlk yükleme hatası (tam ekran hata + tekrar dene); yoksa null. */
    val loadError: String? = null,
) {
    /** Toplam yön sayısı (sabit: 4). */
    val totalSides: Int get() = PhotoSide.entries.size

    /** Çekilmiş sayılan yönler: yerel çekim ∪ sunucuya yüklenmiş. */
    val capturedSides: Set<PhotoSide> get() = capturedPaths.keys + uploadedSides

    /** Çekilmiş yön sayısı (0-4) — "2 / 4 çekildi" sayacı. */
    val capturedCount: Int get() = capturedSides.size

    /** Kalan (çekilmemiş) yön sayısı. */
    val remainingCount: Int get() = (totalSides - capturedCount).coerceAtLeast(0)

    /** 4 yön çekildi mi — "Başlat" burada açılır. */
    val photosComplete: Boolean get() = capturedCount >= totalSides

    /** "Kiralamayı Başlat" aktif mi: 4 yön çekildi + başlatma/iptal/yükleme yok + süre dolmadı. */
    val canStart: Boolean
        get() = photosComplete && !isStarting && !isCancelling && !isLoading && !reservationExpired

    /** "Rezervasyonu İptal Et" aktif mi: başlatma/iptal/yükleme sürmüyor. */
    val canCancel: Boolean
        get() = !isStarting && !isCancelling && !isLoading
}

/**
 * Araç fotoğraf yönü — tasarımdaki Ön/Arka/Sol/Sağ kartları.
 * [apiValue] POST /rentals/{id}/photos side parametresi, [label] kart etiketi,
 * [fileName] uygulama iç depoda çekilen dosyanın adı.
 */
enum class PhotoSide(val apiValue: String, val label: String, val fileName: String) {
    FRONT("FRONT", "Ön", "front.jpg"),
    BACK("BACK", "Arka", "back.jpg"),
    LEFT("LEFT", "Sol", "left.jpg"),
    RIGHT("RIGHT", "Sağ", "right.jpg");

    companion object {
        /** API'den gelen side string'ini enum'a çevirir; tanınmayan değer null. */
        fun fromApi(value: String): PhotoSide? = entries.firstOrNull { it.apiValue == value }
    }
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface RentalPhotosIntent {
    /** İlk yükleme hatası sonrası yeniden dener (rezervasyon/kurtarma yükleme). */
    data object Retry : RentalPhotosIntent

    /** Bir yön fotoğrafı çekildi → yerelde saklanır (yükleme "Başlat"a ertelenir). */
    data class PhotoCaptured(val side: PhotoSide, val path: String) : RentalPhotosIntent

    /** "Kiralamayı Başlat" → POST /rentals → 4 foto upload → POST /rentals/{id}/start. */
    data object StartClicked : RentalPhotosIntent

    /** "Rezervasyonu İptal Et" → DELETE /reservations/{id} (kiralama oluştuysa DELETE /rentals/{id}). */
    data object CancelClicked : RentalPhotosIntent

    /** Üst baştaki geri butonu — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object BackClicked : RentalPhotosIntent

    /** Ekran [RentalPhotosUiState.started] geçişini yaptı → bayrak tüketilir. */
    data object StartedHandled : RentalPhotosIntent

    /** Ekran [RentalPhotosUiState.cancelled] geçişini yaptı → bayrak tüketilir. */
    data object CancelledHandled : RentalPhotosIntent
}
