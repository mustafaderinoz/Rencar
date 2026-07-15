package com.turkcell.rencar.data.model

/**
 * Kiralama — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı). DTO: RentalResponse.
 *
 * Foto ekranı yalnızca kiralama kimliğini ve araç başlığını/plakasını kullanır; başlık ("Marka Model")
 * mapper katmanında ([com.turkcell.rencar.data.mapper.toUi]) oluşturulur.
 */
data class RentalUi(
    val id: String,
    val vehicleTitle: String,
    val vehiclePlate: String,
)
