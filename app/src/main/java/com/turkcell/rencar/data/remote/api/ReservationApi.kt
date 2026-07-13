package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.CreateReservationRequest
import com.turkcell.rencar.data.remote.dto.QuoteResponse
import com.turkcell.rencar.data.remote.dto.ReservationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rezervasyon onayı ekranını besleyen uçlar. Auth zorunlu (AuthInterceptor Bearer ekler),
 * yalnızca CUSTOMER rolü erişebilir (PENDING → 403). Base URL: BuildConfig.BASE_URL.
 *
 * quote openapi'de Vehicles tag'inde yer alır ancak fonksiyonel olarak rezervasyon onayının
 * fiyat önizlemesini beslediğinden burada tutulur (yeni dosya sayısını sınırlamak için, §2.1).
 */
interface ReservationApi {

    /**
     * Fiyat önizleme (VehicleController_quote). SALT HESAP: kayıt oluşturmaz, aracı kilitlemez.
     * plan: PER_MINUTE | HOURLY | DAILY. minutes: tahmini süre (1..43200). Görünmeyen araç → 404.
     */
    @GET("vehicles/{id}/quote")
    suspend fun quote(
        @Path("id") vehicleId: String,
        @Query("plan") plan: String,
        @Query("minutes") minutes: Int,
    ): QuoteResponse

    /**
     * Aracı 15 dk ücretsiz tutar (ReservationController_create); araç RESERVED olur.
     * Araç müsait değilse veya zaten aktif rezervasyon/kiralaman varsa 409.
     */
    @POST("reservations")
    suspend fun create(@Body body: CreateReservationRequest): ReservationResponse
}
