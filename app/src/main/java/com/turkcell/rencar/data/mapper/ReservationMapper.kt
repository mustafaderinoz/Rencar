package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.QuoteUi
import com.turkcell.rencar.data.remote.dto.QuoteResponse

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
