package com.turkcell.rencar.data.model

/**
 * Aktif rezervasyon — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO: [com.turkcell.rencar.data.remote.dto.ReservationResponse].
 *
 * 15 dk ücretsiz tutma süresinin kalanı bu modelden beslenir. [remainingSeconds] sunucunun çağrı
 * anındaki gerçeğidir; rezervasyon ekranı bunu 1 sn'lik yerel sayaçla azaltarak geri sayım gösterir
 * (aktif yolculuk sayacındaki resync kalıbıyla aynı; decisions.md "Aktif Yolculuk"). [vehicleId]
 * yeniden açılışta doğru araç için rezervasyon ekranına kurtarma yönlendirmesinde kullanılır.
 */
data class ReservationUi(
    val reservationId: String,
    val vehicleId: String,
    /** "Renault Clio". */
    val vehicleTitle: String,
    /** "34 RNC 022". */
    val vehiclePlate: String,
    /** Kalan tutma süresi (sn, >= 0); geri sayım bununla başlar. */
    val remainingSeconds: Int,
)
