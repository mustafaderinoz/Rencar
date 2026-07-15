package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.data.remote.dto.VehicleResponse

/**
 * Araç DTO → UI modeli dönüşümü (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 *
 * API şemasındaki değişiklikler (alan adı/tip/yapı) yalnızca burada karşılanır; repository, ViewModel
 * ve UI [VehicleUi] üzerinden çalıştığından etkilenmez ("Minimum Değişiklik İlkesi").
 */
fun VehicleResponse.toUi(): VehicleUi = VehicleUi(
    id = id,
    plate = plate,
    brand = brand,
    model = model,
    type = type,
    pricePerDay = pricePerDay,
    status = status,
    latitude = latitude,
    longitude = longitude,
    pricePerMinute = pricePerMinute,
    pricePerHour = pricePerHour,
    fuelPercent = fuelPercent,
    rangeKm = rangeKm,
    transmission = transmission,
    seats = seats,
    segment = segment,
)

fun List<VehicleResponse>.toUi(): List<VehicleUi> = map(VehicleResponse::toUi)
