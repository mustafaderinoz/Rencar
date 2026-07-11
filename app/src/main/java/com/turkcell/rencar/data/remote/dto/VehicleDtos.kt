package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Araç (Vehicle) akışı DTO'ları — openapi.json şemalarıyla birebir.
 * GET /vehicles → yalnızca AVAILABLE araçları döner (müsaitlik = status AVAILABLE).
 */

/**
 * GET /vehicles 200 yanıt öğesi (VehicleResponseDto).
 * type: SEDAN | SUV | HATCHBACK | STATION | MINIVAN.
 * status: AVAILABLE | RENTED | MAINTENANCE (müşteri ucu yalnız AVAILABLE döndürür).
 * latitude/longitude harita konumlandırması için kullanılır.
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
)
