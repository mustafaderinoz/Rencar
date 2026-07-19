package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.QuoteUi
import com.turkcell.rencar.data.model.ReservationUi
import com.turkcell.rencar.data.remote.api.ReservationApi
import com.turkcell.rencar.data.remote.dto.CreateReservationRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Rezervasyon onayı iş akışı (karar: decisions.md → data + repository + ayrı mapper katmanı).
 * ViewModel → Repository → ReservationApi. Fiyat önizleme DTO'su UI'a doğrudan verilmez; repository
 * ayrı mapper katmanı ([com.turkcell.rencar.data.mapper.toUi]) ile [QuoteUi]'ye çevirir. Rezervasyon
 * sonucu kullanılmadığından [Unit] döner (DTO sızmaz). Hata yönetimi Result ile çağırana taşınır.
 */
@Singleton
class ReservationRepository @Inject constructor(
    private val reservationApi: ReservationApi,
) {
    /**
     * Fiyat önizleme (salt hesap). [plan] PER_MINUTE/HOURLY/DAILY, [minutes] tahmini süre (dk).
     * Görünmeyen/olmayan araç için API 404 döndürür; hata Result olarak çağırana taşınır.
     */
    suspend fun getQuote(vehicleId: String, plan: String, minutes: Int): Result<QuoteUi> =
        runCatching { reservationApi.quote(vehicleId, plan, minutes).toUi() }

    /**
     * Aracı 15 dk ücretsiz rezerve eder (araç RESERVED olur). Araç müsait değilse veya zaten
     * aktif rezervasyon/kiralaman varsa API 409 döndürür; hata Result olarak çağırana taşınır.
     * Yanıt gövdesi (DTO) kullanılmadığından [Unit]'e indirgenir (repo'dan DTO dönmez).
     */
    suspend fun reserve(vehicleId: String): Result<Unit> =
        runCatching { reservationApi.create(CreateReservationRequest(vehicleId)) }.map { }

    /**
     * GET /reservations/active: giriş yapan müşterinin aktif rezervasyonu (kalan süreyle). Aktif
     * rezervasyon yoksa (veya süresi bu çağrıda EXPIRED işlenmişse) API 404 döner; hata Result olarak
     * çağırana taşınır — çağıran bunu "aktif rezervasyon yok" olarak yorumlar (geri sayım gösterilmez).
     */
    suspend fun getActiveReservation(): Result<ReservationUi> =
        runCatching { reservationApi.getActive().toUi() }

    /**
     * DELETE /reservations/{id}: aktif rezervasyonu iptal eder (araç anında AVAILABLE olur). Aksine
     * [com.turkcell.rencar.data.repository.RentalRepository.cancelRental]'daki sessiz temizlikten
     * farklı olarak burada hata KULLANICIYA gösterilir: 204 dışı yanıt (403/404/409) [HttpException]'a
     * çevrilir ki Result.failure olsun (mesaj eşlemesi ViewModel'de [ErrorContext.RESERVATION_CANCEL]).
     */
    suspend fun cancelReservation(reservationId: String): Result<Unit> = runCatching {
        val response = reservationApi.cancel(reservationId)
        if (!response.isSuccessful) throw HttpException(response)
    }
}
