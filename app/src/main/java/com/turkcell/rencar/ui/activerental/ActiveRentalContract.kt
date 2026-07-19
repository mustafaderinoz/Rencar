package com.turkcell.rencar.ui.activerental

import com.turkcell.rencar.data.model.RentalReceiptUi
import com.turkcell.rencar.data.model.VehiclePoint

/**
 * Aktif Yolculuk — saf UI durumu (§4.2).
 *
 * "Kiralamayı Başlat" (POST /rentals/{id}/start) sonrası açılır. Ekran, yolculuğun anlık durumunu
 * GET /rentals/active ile periyodik çeker (currentCost/distanceKm/elapsedSeconds); haritadaki araç
 * konumu Socket.IO 'my-vehicle' akışından ([VehiclePoint]) gelir. Geçen süre, ekranda 1 sn'lik
 * yerel sayaçla akıp her poll'de sunucu değeriyle senkronlanır.
 *
 * Yalnızca saf UI durumunu tutar; navigasyon/framework mekaniği ekran katmanındadır (§4.5–4.6).
 */
data class ActiveRentalUiState(
    /**
     * Simülasyon "Kilitle / Aç" ile başladı mı. Başlamadan poll/sayaç/socket çalışmaz; ekran
     * boş (idle) Content + "kilidi aç" ipucu gösterir. İlk basışta true olur (bkz. [ActiveRentalIntent.LockToggle]).
     */
    val started: Boolean = false,
    /** İlk yükleme (GET /rentals/active) sürüyor. Simülasyon başlamadan false (idle ekran görünür). */
    val isLoading: Boolean = false,
    /** İlk yükleme hatası (tam ekran hata + tekrar dene); yoksa null. */
    val loadError: String? = null,

    /** Aktif kiralama kimliği (nav argümanından; finish için kullanılır). */
    val rentalId: String = "",
    /** "Renault Clio". */
    val vehicleTitle: String = "",
    /** "34 HCH 305". */
    val vehiclePlate: String = "",
    /** Plan etiketi: "Dakikalık" | "Saatlik" | "Günlük". */
    val planLabel: String = "",
    /** "Başlangıç: 14.07.2026 15:55" alt satırı; çözülemezse null (satır gizlenir). */
    val startedAtLabel: String? = null,

    /** Bilgi kartındaki başlangıç ücreti (ör. 15 ₺); anlık ücrete dahildir. */
    val startFee: Double = 0.0,
    /** Kat edilen mesafe (km) — her poll'de güncellenir. */
    val distanceKm: Double = 0.0,
    /** Anlık ücret (₺) — sunucuda hesaplanır, her poll'de güncellenir. */
    val currentCost: Double = 0.0,
    /** Görüntülenen geçen süre (sn) — 1 sn yerel sayaç + poll resync. */
    val elapsedSeconds: Long = 0L,

    /** Socket'ten gelen aracın anlık konumu (harita pin'i); yoksa null. */
    val vehiclePoint: VehiclePoint? = null,

    /**
     * "Kilitle / Aç" YEREL görsel durumu. API'de kilit/aç ucu YOK (openapi.json'da yoktur), bu yüzden
     * buton ağ çağrısı yapmaz; yalnız bu bayrağı çevirir (simülasyon). Uç eklenince buraya bağlanır.
     */
    val locked: Boolean = true,

    /** POST /rentals/{id}/finish sürüyor (buton spinner). */
    val isFinishing: Boolean = false,
    /** Bitirme hatası (buton üstünde gösterilir); yoksa null. */
    val finishError: String? = null,
    /** Yolculuk bitiş dökümü; dolduğunda ekran "bitti" özetine geçer (ödeme ayrı adım). */
    val receipt: RentalReceiptUi? = null,
) {
    /** Yolculuk bitti mi (finish başarılı) — canlı sayaç/poll durur, ekranda özet kalır. */
    val isFinished: Boolean get() = receipt != null
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface ActiveRentalIntent {
    /** İlk yükleme hatası sonrası yeniden dener (GET /rentals/active poll'ünü baştan başlatır). */
    data object Retry : ActiveRentalIntent

    /** "Kiralamayı Bitir" → POST /rentals/{id}/finish (yolculuk + simülasyon sonlanır). */
    data object FinishClicked : ActiveRentalIntent

    /**
     * "Kilitle / Aç" → yerel görsel kilit durumunu çevirir (API ucu yok). **İlk basış** ayrıca
     * simülasyonu (poll + sayaç + socket) başlatır; sonraki basışlar yalnız görsel toggle'dır.
     */
    data object LockToggle : ActiveRentalIntent

    /** Üst baştaki geri butonu — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object BackClicked : ActiveRentalIntent
}
