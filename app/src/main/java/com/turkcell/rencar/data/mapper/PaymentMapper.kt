package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.PaymentReceiptUi
import com.turkcell.rencar.data.model.PaymentResultUi
import com.turkcell.rencar.data.remote.dto.CardResponse
import com.turkcell.rencar.data.remote.dto.PayRentalResponse
import com.turkcell.rencar.data.remote.dto.RentalResponse
import com.turkcell.rencar.data.remote.dto.WalletResponse

/**
 * Ödeme akışı DTO → UI modeli dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * Kart SKT etiketi, ücret kalem türetimi ("Kiralama ücreti" = totalPrice − startFee − serviceFee) ve
 * plan etiketi burada üretilir; API şema değişiklikleri yalnızca bu katmanda karşılanır.
 */

fun CardResponse.toUi(): CardUi = CardUi(
    id = id,
    brand = brand,
    last4 = last4,
    expLabel = "%02d/%02d".format(expMonth, expYear % 100),
    isDefault = isDefault,
)

/** GET /wallet → yalnız cüzdan bakiyesi (transactions bu akışta kullanılmaz). */
fun WalletResponse.toBalance(): Double = balance

/**
 * GET /rentals/{id} → ödeme ekranı üst özeti. Kiralama (kullanım) ücreti kalemi türetilir; toplam
 * null gelirse (beklenmez, bitmiş kiralamada dolu) startFee'ye indirgenir.
 */
fun RentalResponse.toPaymentReceipt(): PaymentReceiptUi {
    val service = serviceFee ?: 0.0
    val total = totalPrice ?: startFee
    val usage = (total - startFee - service).coerceAtLeast(0.0)
    return PaymentReceiptUi(
        rentalId = id,
        vehicleTitle = "${vehicle.brand} ${vehicle.model}",
        vehiclePlate = vehicle.plate,
        planLabel = paymentPlanLabel(plan),
        durationMinutes = durationMinutes.toInt(),
        distanceKm = distanceKm,
        usageFee = usage,
        startFee = startFee,
        serviceFee = service,
        totalPrice = total,
        alreadyPaid = paymentStatus.equals("PAID", ignoreCase = true),
    )
}

fun PayRentalResponse.toResult(): PaymentResultUi = PaymentResultUi(
    paidAmount = paidAmount,
    discountAmount = discountAmount,
    method = method,
    walletBalance = walletBalance,
    cardBrand = card?.brand,
    cardLast4 = card?.last4,
)

/** API plan enum'unu tasarımdaki Türkçe etikete çevirir (data katmanı). */
private fun paymentPlanLabel(plan: String): String = when (plan.uppercase()) {
    "PER_MINUTE" -> "Dakikalık"
    "HOURLY" -> "Saatlik"
    "DAILY" -> "Günlük"
    else -> plan
}
