package com.turkcell.rencar.data.model

/**
 * Ödeme akışı — UI/domain modelleri (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO'lar: [com.turkcell.rencar.data.remote.dto.CardResponse],
 * [com.turkcell.rencar.data.remote.dto.RentalResponse],
 * [com.turkcell.rencar.data.remote.dto.PayRentalResponse].
 *
 * Ekran ham API alanı (plan enum'u, SKT ay/yıl sayıları) tutmaz; etiketler mapper'da üretilir.
 */

/** Kayıtlı kart (ödeme yöntemi listesi). [expLabel] "08/28" biçimi; [isDefault] öntanımlı kart. */
data class CardUi(
    val id: String,
    /** VISA | MASTERCARD (ham). Görsel rozet çağrı yerinde bu değere göre çizilir. */
    val brand: String,
    /** Kartın son 4 hanesi (ör. "4291"). */
    val last4: String,
    /** Son kullanma etiketi "AA/YY" (ör. "08/28"). */
    val expLabel: String,
    val isDefault: Boolean,
)

/**
 * Bitmiş kiralamanın ücret dökümü (ödeme ekranı üst özeti). GET /rentals/{id}'den türetilir:
 * [usageFee] = totalPrice − startFee − serviceFee (Kiralama ücreti). [alreadyPaid] true ise ödeme
 * zaten alınmış (tekrar ödemeye kapalı).
 */
data class PaymentReceiptUi(
    val rentalId: String,
    /** "Renault Clio". */
    val vehicleTitle: String,
    /** "34 HCH 305". */
    val vehiclePlate: String,
    /** Plan etiketi: "Dakikalık" | "Saatlik" | "Günlük". */
    val planLabel: String,
    /** Faturalanan süre (dk) — "Kiralama ücreti (N dk)" etiketinde ve "Süre" kutusunda gösterilir. */
    val durationMinutes: Int,
    /** Kat edilen mesafe (km) — üstteki "Mesafe" kutusunda gösterilir. */
    val distanceKm: Double,
    /** Kiralama (kullanım) ücreti = totalPrice − startFee − serviceFee, en az 0. */
    val usageFee: Double,
    /** Açılış (başlangıç) ücreti. */
    val startFee: Double,
    /** Hizmet bedeli (servis ücreti); null gelirse 0. */
    val serviceFee: Double,
    /** Ödenecek toplam tutar (indirim öncesi). */
    val totalPrice: Double,
    /** Ödeme zaten alınmış mı (paymentStatus == PAID). */
    val alreadyPaid: Boolean,
)

/**
 * Ödeme sonucu makbuzu (POST /rentals/{id}/pay). [paidAmount] fiilen ödenen (indirim sonrası);
 * WALLET'ta [walletBalance], CARD'da [cardBrand]/[cardLast4] dolu olur.
 */
data class PaymentResultUi(
    val paidAmount: Double,
    val discountAmount: Double,
    /** WALLET | CARD (ödenen yöntem). */
    val method: String,
    /** Ödeme sonrası cüzdan bakiyesi — yalnız WALLET yönteminde. */
    val walletBalance: Double?,
    /** Kullanılan kart markası — yalnız CARD yönteminde. */
    val cardBrand: String?,
    /** Kullanılan kartın son 4 hanesi — yalnız CARD yönteminde. */
    val cardLast4: String?,
)
