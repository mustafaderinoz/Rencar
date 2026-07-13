package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Araç (Vehicle) akışı DTO'ları — openapi.json şemalarıyla birebir.
 * GET /vehicles → varsayılan yalnızca AVAILABLE araçları döner (includeBusy=true ile RENTED/RESERVED de gelir).
 * GET /vehicles/{id} → tek aracın detayı (araç detay ekranını besler).
 */

/**
 * VehicleResponseDto — GET /vehicles ve GET /vehicles/{id} yanıt öğesi.
 *
 * type: SEDAN | SUV | HATCHBACK | STATION | MINIVAN.
 * transmission: MANUAL | AUTOMATIC. segment: ECONOMY | COMFORT | SUV.
 * status: AVAILABLE | RESERVED | RENTED | MAINTENANCE (müşteri ucu MAINTENANCE döndürmez).
 * fuelPercent 0..100 (yakıt barı), rangeKm tahmini menzil, seats koltuk sayısı.
 * pricePerMinute/pricePerHour/pricePerDay → detaydaki fiyat satırı.
 * latitude/longitude harita konumlandırması + uzaklık hesabı için kullanılır.
 *
 * NOT (§2.2): Aşağıdaki alanlar güncel openapi.json'da var ANCAK canlı sunucu bunları HENÜZ
 * döndürmüyor (deploy openapi'nin gerisinde). Bu yüzden nullable + varsayılan null tutulurlar;
 * böylece hem eski hem yeni yanıt deserialize olur (aksi halde MissingFieldException → liste boş).
 * Backend güncellenince değer otomatik dolar, kod değişmez. `pricePerDay` ve `status` eski
 * sunucuda da mevcut olduğundan non-null kalır.
 */
@Serializable
data class VehicleResponse(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val type: String,
    val pricePerDay: Double,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val createdAt: String,
    val updatedAt: String,
    // ── Canlı sunucuda henüz olmayabilen alanlar (backend deploy'una kadar null gelir) ──
    val pricePerMinute: Double? = null,
    val pricePerHour: Double? = null,
    val fuelPercent: Double? = null,
    val rangeKm: Double? = null,
    val transmission: String? = null,
    val seats: Int? = null,
    val segment: String? = null,
)
