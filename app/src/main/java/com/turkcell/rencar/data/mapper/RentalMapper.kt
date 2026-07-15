package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.ActiveRentalUi
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.RentalReceiptUi
import com.turkcell.rencar.data.model.RentalUi
import com.turkcell.rencar.data.remote.dto.ActiveRentalResponse
import com.turkcell.rencar.data.remote.dto.FinishRentalResponse
import com.turkcell.rencar.data.remote.dto.RentalPhotosState
import com.turkcell.rencar.data.remote.dto.RentalResponse
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Kiralama DTO'ları → UI modeli dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * Araç başlığı ("Marka Model") burada oluşturulur; API şema değişiklikleri yalnızca burada karşılanır.
 */
fun RentalResponse.toUi(): RentalUi = RentalUi(
    id = id,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    vehiclePlate = vehicle.plate,
)

fun RentalPhotosState.toUi(): RentalPhotosUi = RentalPhotosUi(
    uploadedSides = photos.map { it.side },
    uploadedCount = uploadedCount,
    photosComplete = photosComplete,
)

fun ActiveRentalResponse.toUi(): ActiveRentalUi = ActiveRentalUi(
    rentalId = id,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    vehiclePlate = vehicle.plate,
    planLabel = planLabel(plan),
    startedAtLabel = formatIsoDateTime(startedAt),
    startFee = startFee,
    distanceKm = distanceKm,
    elapsedSeconds = elapsedSeconds.toLong(),
    currentCost = currentCost,
)

fun FinishRentalResponse.toUi(): RentalReceiptUi = RentalReceiptUi(
    rentalId = id,
    startFee = startFee,
    usageFee = usageFee,
    distanceKm = distanceKm,
    elapsedSeconds = elapsedSeconds.toLong(),
    totalPrice = totalPrice ?: (startFee + usageFee),
)

/** API plan enum'unu tasarımdaki Türkçe etikete çevirir (data katmanı; ui/reservation'a bağlanmaz). */
private fun planLabel(plan: String): String = when (plan.uppercase()) {
    "PER_MINUTE" -> "Dakikalık"
    "HOURLY" -> "Saatlik"
    "DAILY" -> "Günlük"
    else -> plan
}

/**
 * ISO-8601 tarih-saat'i cihaz yerel saatinde "dd.MM.yyyy HH:mm" biçimine çevirir. minSdk 24 +
 * core library desugaring kapalı olduğundan java.time yerine [SimpleDateFormat] kullanılır.
 * 'X' deseni ISO ofsetini ("Z" veya "+03:00") çözer; çözülemezse null (etiket UI'da gizlenir).
 */
private fun formatIsoDateTime(iso: String): String? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )
    for (pattern in patterns) {
        val parsed = runCatching { SimpleDateFormat(pattern, Locale.US).parse(iso) }.getOrNull()
        if (parsed != null) {
            return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(parsed)
        }
    }
    return null
}
