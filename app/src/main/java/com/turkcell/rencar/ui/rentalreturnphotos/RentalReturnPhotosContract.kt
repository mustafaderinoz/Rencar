package com.turkcell.rencar.ui.rentalreturnphotos

/**
 * Araç teslim durumu (kiralama sonrası fotoğraf) — saf UI durumu (§4.2).
 *
 * Aktif Yolculuk ekranındaki "Kiralamayı Bitir" (POST /rentals/{id}/finish) BAŞARILI olduktan
 * sonra açılır: ücret o anda kilitlendiği için burada geçen süre faturalanmaz. 4 yön (Ön/Arka/
 * Sol/Sağ) çekilince "Ödeme Ekranına Geç" açılır ve payment/{rentalId} ekranına geçilir.
 *
 * MOCK: Teslim fotoğrafı için backend'de UÇ YOKTUR — `POST /rentals/{id}/photos` yalnız PREPARING
 * aşaması içindir (sonrasında 409 döner). Bu yüzden fotoğraflar YALNIZCA cihazda tutulur, ağa
 * gönderilmez (§2.2: var olmayan uç uydurulmaz). Uç eklendiğinde tek nokta değişir: ViewModel'e
 * repository çağrısı; ekran/tasarım/navigasyon aynı kalır.
 *
 * Alanlar yalnızca UI durumunu tutar; navigasyon ekran katmanındadır (§4.5–4.6).
 */
data class RentalReturnPhotosUiState(
    /** Ödemesi yapılacak kiralamanın kimliği (nav argümanından). */
    val rentalId: String = "",
    /** Başlık: "Renault Clio" (araç özeti — Aktif Yolculuk ekranından taşınır). */
    val vehicleTitle: String = "",
    /** Alt başlık: "34 RNC 022" (plaka). */
    val vehiclePlate: String = "",
    /** Çekilmiş yönlerin yerel dosya yolları (önizleme/onay rozeti için). */
    val capturedPaths: Map<ReturnPhotoSide, String> = emptyMap(),
) {
    /** Toplam yön sayısı (sabit: 4). */
    val totalSides: Int get() = ReturnPhotoSide.entries.size

    /** Çekilen yön sayısı (0-4) — "2 / 4 çekildi" sayacı. */
    val capturedCount: Int get() = capturedPaths.size

    /** Kalan (çekilmemiş) yön sayısı. */
    val remainingCount: Int get() = (totalSides - capturedCount).coerceAtLeast(0)

    /** "Ödeme Ekranına Geç" aktif mi: 4 yön tamam. */
    val canContinue: Boolean get() = capturedCount == totalSides
}

/**
 * Teslim fotoğrafı yönü — tasarımdaki Ön/Arka/Sol/Sağ kartları.
 * [label] kart etiketi, [fileName] uygulama iç depoda çekilen dosyanın adı.
 *
 * Kiralama öncesi akışın [com.turkcell.rencar.ui.rentalphotos.PhotoSide] enum'undan AYRIDIR:
 * orada `apiValue` (POST side parametresi) vardır, burada yükleme olmadığı için yoktur.
 */
enum class ReturnPhotoSide(val label: String, val fileName: String) {
    FRONT("Ön", "return-front.jpg"),
    BACK("Arka", "return-back.jpg"),
    LEFT("Sol", "return-left.jpg"),
    RIGHT("Sağ", "return-right.jpg"),
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface RentalReturnPhotosIntent {
    /** Bir yön fotoğrafı çekildi → yerel state'e işlenir (ağ çağrısı yok — mock). */
    data class PhotoCaptured(val side: ReturnPhotoSide, val path: String) : RentalReturnPhotosIntent

    /** "Ödeme Ekranına Geç" — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object ContinueClicked : RentalReturnPhotosIntent

    /** Üst baştaki geri butonu — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object BackClicked : RentalReturnPhotosIntent
}
