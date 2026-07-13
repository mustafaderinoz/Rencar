package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Rezervasyon akışı DTO'ları — openapi.json şemalarıyla birebir.
 *
 * Rezervasyon onayı ekranını iki uç besler:
 * - GET /vehicles/{id}/quote → fiyat önizleme ("Başlangıç ücreti" + "Tahmini ücret" satırları).
 * - POST /reservations → aracı 15 dk ÜCRETSİZ tutar (vehicle RESERVED olur), "Rezervasyonu Tamamla".
 */

/** POST /reservations gövdesi (CreateReservationDto). */
@Serializable
data class CreateReservationRequest(
    val vehicleId: String,
)

/**
 * POST /reservations 201 & GET /reservations/active 200 yanıtı (ReservationResponseDto).
 * status: ACTIVE | CONVERTED | CANCELLED | EXPIRED. remainingSeconds: süre dolmasına kalan (>=0).
 */
@Serializable
data class ReservationResponse(
    val id: String,
    val userId: String,
    val vehicleId: String,
    val vehicle: ReservationVehicleSummary,
    val status: String,
    val expiresAt: String,
    val remainingSeconds: Int,
    val createdAt: String,
)

/** Rezervasyon yanıtındaki araç özeti (ReservationVehicleSummaryDto). */
@Serializable
data class ReservationVehicleSummary(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val pricePerMinute: Double,
)

/**
 * GET /vehicles/{id}/quote 200 yanıtı (QuoteResponseDto). SALT HESAP: kayıt oluşturmaz.
 * plan: PER_MINUTE | HOURLY | DAILY. usageFee kullanım ücreti, startFee açılış (DAILY'de 0),
 * serviceFee servis ücreti (DAILY'de 0), estimatedTotal tahmini toplam.
 */
@Serializable
data class QuoteResponse(
    val vehicleId: String,
    val plan: String,
    val minutes: Int,
    val usageFee: Double,
    val startFee: Double,
    val serviceFee: Double,
    val estimatedTotal: Double,
)
