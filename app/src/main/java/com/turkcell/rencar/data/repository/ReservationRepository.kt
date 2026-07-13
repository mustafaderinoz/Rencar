package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.remote.api.ReservationApi
import com.turkcell.rencar.data.remote.dto.CreateReservationRequest
import com.turkcell.rencar.data.remote.dto.QuoteResponse
import com.turkcell.rencar.data.remote.dto.ReservationResponse
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rezervasyon onayı iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → ReservationApi. Hata yönetimi Result ile çağırana taşınır
 * (mesaj eşlemesi ViewModel'de). Ayrı domain/UseCase katmanı eklenmez (AGENTS §4.6).
 */
@Singleton
class ReservationRepository @Inject constructor(
    private val reservationApi: ReservationApi,
) {
    /**
     * Fiyat önizleme (salt hesap). [plan] PER_MINUTE/HOURLY/DAILY, [minutes] tahmini süre (dk).
     * Görünmeyen/olmayan araç için API 404 döndürür; hata Result olarak çağırana taşınır.
     */
    suspend fun getQuote(vehicleId: String, plan: String, minutes: Int): Result<QuoteResponse> =
        runCatching { reservationApi.quote(vehicleId, plan, minutes) }

    /**
     * Aracı 15 dk ücretsiz rezerve eder (araç RESERVED olur). Araç müsait değilse veya zaten
     * aktif rezervasyon/kiralaman varsa API 409 döndürür; hata Result olarak çağırana taşınır.
     */
    suspend fun reserve(vehicleId: String): Result<ReservationResponse> =
        runCatching { reservationApi.create(CreateReservationRequest(vehicleId)) }
}
