package com.turkcell.rencar.data.model

/**
 * Aktif yolculuk — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO: [com.turkcell.rencar.data.remote.dto.ActiveRentalResponse].
 *
 * Plan etiketi ("Dakikalık") ve başlangıç zaman etiketi mapper'da üretilir; ekran ham API alanı
 * (plan enum'u, ISO tarih) tutmaz. Böylece şema değişiklikleri tek noktada (mapper) emilir.
 */
data class ActiveRentalUi(
    val rentalId: String,
    /** "Renault Clio". */
    val vehicleTitle: String,
    /** "34 HCH 305". */
    val vehiclePlate: String,
    /** Plan etiketi: "Dakikalık" | "Saatlik" | "Günlük". */
    val planLabel: String,
    /** Başlangıç zamanı (cihaz yerel), "14.07.2026 15:55" biçiminde; çözülemezse null. */
    val startedAtLabel: String?,
    /** Bilgi kartındaki başlangıç ücreti (ör. 15 ₺); [currentCost]'a dahildir. */
    val startFee: Double,
    /** Anlık kat edilen mesafe (km). */
    val distanceKm: Double,
    /** Sunucudaki geçen süre (sn); ekran yerel sayacı her poll'de bununla resync eder. */
    val elapsedSeconds: Long,
    /** Anlık ücret (₺) — sunucuda hesaplanır. */
    val currentCost: Double,
)

/**
 * Yolculuk bitiş özeti — UI/domain modeli. DTO:
 * [com.turkcell.rencar.data.remote.dto.FinishRentalResponse].
 * "Kiralamayı Bitir" sonrası ekranda gösterilecek kesin döküm (ödeme ayrı adım).
 */
data class RentalReceiptUi(
    val rentalId: String,
    val startFee: Double,
    val usageFee: Double,
    val distanceKm: Double,
    val elapsedSeconds: Long,
    /** API toplamı; null gelirse startFee + usageFee'den türetilir. */
    val totalPrice: Double,
)
