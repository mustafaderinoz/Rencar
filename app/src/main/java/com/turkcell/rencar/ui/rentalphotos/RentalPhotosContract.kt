package com.turkcell.rencar.ui.rentalphotos

/**
 * Araç durumu (kiralama öncesi fotoğraf) — saf UI durumu (§4.2).
 *
 * Rezervasyon "Tamamla" (Dakikalık/Saatlik plan) sonrası açılır. Ekran açılışında
 * POST /rentals ile kiralama PREPARING olarak oluşturulur (rentalId + araç özeti alınır),
 * ardından 4 yön (Ön/Arka/Sol/Sağ) POST /rentals/{id}/photos ile yüklenir. 4 yön tamamlanınca
 * "Kiralamayı Başlat" açılır (şimdilik yalnız Home'a döner; start ucu çağrılmaz).
 *
 * Alanlar yalnızca UI durumunu tutar; navigasyon ekran katmanındadır (§4.5–4.6).
 */
data class RentalPhotosUiState(
    /** POST /rentals sürüyor (ilk açılış). Başarısızsa [createError] dolar. */
    val isCreating: Boolean = true,
    /** Oluşturulan kiralamanın kimliği; foto yüklemede path olarak kullanılır. */
    val rentalId: String? = null,
    /** Başlık: "Renault Clio" (araç özeti). */
    val vehicleTitle: String = "",
    /** Alt başlık: "34 RNC 022" (plaka). */
    val vehiclePlate: String = "",
    /** Çekilmiş yönlerin yerel dosya yolları (önizleme küçük resmi için). */
    val capturedPaths: Map<PhotoSide, String> = emptyMap(),
    /** Sunucuya başarıyla yüklenmiş yönler (yeşil onay rozeti). */
    val uploadedSides: Set<PhotoSide> = emptySet(),
    /** O an yüklenen yön (kartta spinner); yoksa null. */
    val uploadingSide: PhotoSide? = null,
    /** Sunucudaki yüklenen yön sayısı (0-4) — "2 / 4 çekildi" sayacı. */
    val uploadedCount: Int = 0,
    /** 4 yön tamamlandı mı (POST cevabından) — "Başlat" burada açılır. */
    val photosComplete: Boolean = false,
    /** Tek foto yükleme veya başlatma hatası (kart altında gösterilir); yoksa null. */
    val errorMessage: String? = null,
    /** Kiralama oluşturma hatası (tam ekran hata + tekrar dene); yoksa null. */
    val createError: String? = null,
    /** POST /rentals/{id}/start sürüyor (buton spinner). */
    val isStarting: Boolean = false,
    /** Yolculuk başarıyla başladı (ACTIVE) → geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val started: Boolean = false,
) {
    /** Toplam yön sayısı (sabit: 4). */
    val totalSides: Int get() = PhotoSide.entries.size

    /** Kalan (yüklenmemiş) yön sayısı. */
    val remainingCount: Int get() = (totalSides - uploadedCount).coerceAtLeast(0)

    /** "Kiralamayı Başlat" aktif mi: 4 yön tamam + yükleme/başlatma sürmüyor. */
    val canStart: Boolean
        get() = photosComplete && uploadingSide == null && !isCreating && !isStarting
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
    /** Kiralama oluşturma hatası sonrası yeniden dener (POST /rentals). */
    data object Retry : RentalPhotosIntent

    /** Bir yön fotoğrafı çekildi → POST /rentals/{id}/photos ile yüklenir. */
    data class PhotoCaptured(val side: PhotoSide, val path: String) : RentalPhotosIntent

    /** "Kiralamayı Başlat" → POST /rentals/{id}/start (yolculuk ACTIVE olur). */
    data object StartClicked : RentalPhotosIntent

    /** Üst baştaki geri butonu — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object BackClicked : RentalPhotosIntent
}
