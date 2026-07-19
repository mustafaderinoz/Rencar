package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.ActiveRentalUi
import com.turkcell.rencar.data.model.RentalHistoryItemUi
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.RentalReceiptUi
import com.turkcell.rencar.data.model.RentalStatsUi
import com.turkcell.rencar.data.model.RentalUi
import com.turkcell.rencar.data.model.ResumableRentalUi
import com.turkcell.rencar.data.remote.dto.ActiveRentalResponse
import com.turkcell.rencar.data.remote.dto.FinishRentalResponse
import com.turkcell.rencar.data.remote.dto.RentalPhotosState
import com.turkcell.rencar.data.remote.dto.RentalResponse
import com.turkcell.rencar.data.remote.dto.RentalStatsResponse
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Kiralama DTO'ları → UI modeli dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * Araç başlığı ("Marka Model") burada oluşturulur; API şema değişiklikleri yalnızca burada karşılanır.
 */
fun RentalResponse.toUi(): RentalUi = RentalUi(
    id = id,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    vehiclePlate = vehicle.plate,
)

/**
 * GET /rentals kaydı → Kiralamalarım kartı. Tutar yalnız kilitlenmişse ([totalPrice] != null; tamamlanmış
 * yolculuk) gösterilir; aksi halde durum etiketi gösterilir (yeni açılan kiralamalar da listede görünür).
 * Kart tarihi tercihen [startedAt] (yolculuğun başladığı an), yoksa [createdAt] üzerinden biçimlenir.
 */
fun RentalResponse.toHistoryItem(): RentalHistoryItemUi = RentalHistoryItemUi(
    id = id,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    dateLabel = formatTripDate(startedAt ?: createdAt),
    durationLabel = "${durationMinutes.roundToInt()} dk",
    distanceLabel = "${formatKm(distanceKm)} km",
    priceLabel = totalPrice?.let { formatTl(it) },
    statusLabel = if (totalPrice != null) null else statusLabel(status),
)

/** GET /rentals/stats → başlık özeti ("Bu ay 6 yolculuk · ₺612 harcama"; harcama tam TL). */
fun RentalStatsResponse.toUi(): RentalStatsUi {
    val trips = tripCount.roundToInt()
    return RentalStatsUi(
        tripCount = trips,
        summaryLabel = "Bu ay $trips yolculuk · ${formatTlWhole(totalSpent)} harcama",
    )
}

fun RentalPhotosState.toUi(): RentalPhotosUi = RentalPhotosUi(
    uploadedSides = photos.map { it.side },
    uploadedCount = uploadedCount,
    photosComplete = photosComplete,
)

/**
 * Devam eden kiralama DTO → devralma/kurtarma modeli. [isActive] durumdan türetilir (ACTIVE → true;
 * PREPARING → false). Araç başlığı ("Marka Model") burada oluşturulur (foto devralma ekranı başlığı).
 */
fun RentalResponse.toResumableUi(): ResumableRentalUi = ResumableRentalUi(
    rentalId = id,
    vehicleId = vehicleId,
    plan = plan,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    vehiclePlate = vehicle.plate,
    isActive = status.equals("ACTIVE", ignoreCase = true),
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

/** Kiralamalarım kartındaki durum etiketi (tutarı olmayan kayıtlar için). */
private fun statusLabel(status: String): String = when (status.uppercase()) {
    "ACTIVE" -> "Aktif"
    "PREPARING" -> "Hazırlanıyor"
    "CANCELLED" -> "İptal edildi"
    "COMPLETED" -> "Tamamlandı"
    else -> status
}

/** Mesafe: "12,4 km" (tek ondalık, Türkçe virgül). */
private fun formatKm(value: Double): String = "%.1f".format(Locale.forLanguageTag("tr"), value)

/** "₺612" — tam TL (kuruşsuz), başlık özeti için; Türkçe binlik ayraç. */
private fun formatTlWhole(value: Double): String = "₺%,.0f".format(Locale.forLanguageTag("tr"), value)

/**
 * ISO-8601 tarih-saat'i cihaz yerel saatinde "dd.MM.yyyy HH:mm" biçimine çevirir. minSdk 24 +
 * core library desugaring kapalı olduğundan java.time yerine [SimpleDateFormat] kullanılır.
 * Çözülemezse null (etiket UI'da gizlenir).
 */
private fun formatIsoDateTime(iso: String): String? {
    val parsed = parseIso(iso) ?: return null
    return SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(parsed)
}

/** Kiralamalarım kart tarihi: "26 Haz 2026 · 14:32" (Türkçe ay kısaltması). Çözülemezse boş etiket. */
private fun formatTripDate(iso: String?): String {
    val parsed = iso?.let { parseIso(it) } ?: return ""
    return SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("tr")).format(parsed)
}
