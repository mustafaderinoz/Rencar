package com.turkcell.rencar.data.model

/**
 * Fiyat önizleme — UI modeli (decisions.md → "Katman Derinliği" mapper katmanı).
 *
 * API DTO'su [com.turkcell.rencar.data.remote.dto.QuoteResponse] UI'a doğrudan verilmez; repository
 * ayrı mapper katmanı ([com.turkcell.rencar.data.mapper.toUi]) ile bu modele çevirir.
 */
data class QuoteUi(
    val vehicleId: String,
    val plan: String,
    val minutes: Int,
    val usageFee: Double,
    val startFee: Double,
    val serviceFee: Double,
    val estimatedTotal: Double,
)
