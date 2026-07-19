package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.QuoteUi
import com.turkcell.rencar.data.model.ReservationUi
import com.turkcell.rencar.data.remote.dto.QuoteResponse
import com.turkcell.rencar.data.remote.dto.ReservationResponse

/**
 * Fiyat önizleme DTO → UI modeli dönüşümü (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * API şema değişiklikleri yalnızca burada karşılanır ("Minimum Değişiklik İlkesi").
 */
fun QuoteResponse.toUi(): QuoteUi = QuoteUi(
    vehicleId = vehicleId,
    plan = plan,
    minutes = minutes,
    usageFee = usageFee,
    startFee = startFee,
    serviceFee = serviceFee,
    estimatedTotal = estimatedTotal,
)

/**
 * Aktif rezervasyon DTO → UI modeli. Araç başlığı ("Marka Model") burada üretilir; kalan süre
 * defansif olarak 0'ın altına inmez (sunucu >= 0 döndürür; VehicleResponse extras kalıbı).
 */
fun ReservationResponse.toUi(): ReservationUi = ReservationUi(
    reservationId = id,
    vehicleId = vehicleId,
    vehicleTitle = "${vehicle.brand} ${vehicle.model}",
    vehiclePlate = vehicle.plate,
    remainingSeconds = remainingSeconds.coerceAtLeast(0),
)
