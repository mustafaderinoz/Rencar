package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.RentalUi
import com.turkcell.rencar.data.remote.dto.RentalPhotosState
import com.turkcell.rencar.data.remote.dto.RentalResponse

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
