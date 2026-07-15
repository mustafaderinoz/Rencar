package com.turkcell.rencar.data.model

/**
 * Araç — UI/domain modeli (decisions.md → "Katman Derinliği" + "Minimum Değişiklik İlkesi").
 *
 * API DTO'su [com.turkcell.rencar.data.remote.dto.VehicleResponse] UI'a doğrudan verilmez;
 * repository, ayrı mapper katmanı ([com.turkcell.rencar.data.mapper.toUi]) üzerinden bu modele
 * çevirir. Böylece backend şema değişikliği tek noktada (mapper) emilir; UI/ViewModel değişmez.
 *
 * Alan adları/tipleri şu an DTO ile paraleldir (mapping birebir kopya); breaking bir API değişimi
 * olursa yalnızca DTO + mapper değişir, bu model ve tüm UI aynı kalır.
 */
data class VehicleUi(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val type: String,
    val pricePerDay: Double,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val pricePerMinute: Double? = null,
    val pricePerHour: Double? = null,
    val fuelPercent: Double? = null,
    val rangeKm: Double? = null,
    val transmission: String? = null,
    val seats: Int? = null,
    val segment: String? = null,
)
