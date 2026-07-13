package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Ehliyet (License) akışı DTO'ları — openapi.json şemalarıyla birebir.
 * POST /license/upload → LicenseResponseDto (durum UNDER_REVIEW).
 */

/**
 * POST /license/upload 201 yanıtı (LicenseResponseDto).
 * status: NOT_SUBMITTED | UNDER_REVIEW | APPROVED | REJECTED.
 * rejectReason/reviewedAt yükleme anında null olur (nullable alanlar `explicitNulls=false`
 * ile atlanabildiğinden varsayılan null verilir).
 */
@Serializable
data class LicenseResponse(
    val id: String,
    val status: String,
    val frontImageUrl: String,
    val backImageUrl: String,
    val rejectReason: String? = null,
    val reviewedAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * GET /license/status 200 yanıtı (LicenseStatusResponseDto).
 * status: NOT_SUBMITTED (hiç yükleme yok) | UNDER_REVIEW (admin onayı bekliyor) |
 * APPROVED (onaylı, rol CUSTOMER) | REJECTED (reddedildi, rejectReason dolu).
 * Diğer alanlar NOT_SUBMITTED iken null olabilir (explicitNulls=false ile atlanır).
 */
@Serializable
data class LicenseStatusResponse(
    val status: String,
    val frontImageUrl: String? = null,
    val backImageUrl: String? = null,
    val rejectReason: String? = null,
    val reviewedAt: String? = null,
)
