package com.turkcell.rencar.data.model

/**
 * Kiralama foto akışı durumu — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO: RentalPhotosState. [uploadedSides] yüklenmiş yön kodları (FRONT | BACK | LEFT | RIGHT).
 */
data class RentalPhotosUi(
    val uploadedSides: List<String>,
    val uploadedCount: Int,
    val photosComplete: Boolean,
)
