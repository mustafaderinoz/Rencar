package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.ActiveRentalResponse
import com.turkcell.rencar.data.remote.dto.CreateRentalRequest
import com.turkcell.rencar.data.remote.dto.FinishRentalResponse
import com.turkcell.rencar.data.remote.dto.PayRentalRequest
import com.turkcell.rencar.data.remote.dto.PayRentalResponse
import com.turkcell.rencar.data.remote.dto.RentalPhotosState
import com.turkcell.rencar.data.remote.dto.RentalResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Kiralama uçları (openapi.json — tag: Rentals). Auth zorunlu (AuthInterceptor Bearer ekler),
 * yalnızca CUSTOMER rolü erişebilir. Base URL: BuildConfig.BASE_URL.
 */
interface RentalApi {

    /**
     * Kiralamayı açar (RentalController_create). Dakikalık/Saatlik planda kayıt PREPARING olur:
     * araç kilitlenir ama süre işlemez. KURAL: araçta aktif rezervasyon yoksa 409 döner.
     */
    @POST("rentals")
    suspend fun create(@Body body: CreateRentalRequest): RentalResponse

    /**
     * PREPARING kiralamaya bir yönün (FRONT/BACK/LEFT/RIGHT) fotoğrafını yükler
     * (RentalController_uploadPhoto, multipart/form-data). Aynı yöne ikinci yükleme öncekini
     * değiştirir. Cevap akışın anlık durumudur ("2/4 çekildi" sayacı buradan beslenir).
     */
    @Multipart
    @POST("rentals/{id}/photos")
    suspend fun uploadPhoto(
        @Path("id") rentalId: String,
        @Part side: MultipartBody.Part,
        @Part file: MultipartBody.Part,
    ): RentalPhotosState

    /**
     * PREPARING yolculuğu ACTIVE yapar (RentalController_start). 4 yönün tamamı yüklenmemişse
     * 409 (kalan sayıyla) döner; startedAt bu anda atılır (foto süresi faturalanmaz).
     */
    @POST("rentals/{id}/start")
    suspend fun start(@Path("id") rentalId: String): RentalResponse

    /**
     * Kullanıcının AKTİF yolculuğunun anlık durumu (RentalController_active). Aktif Kiralama ekranı
     * periyodik çeker: currentCost/distanceKm/elapsedSeconds canlı ilerler. Aktif yolculuk yoksa 404.
     */
    @GET("rentals/active")
    suspend fun getActiveRental(): ActiveRentalResponse

    /**
     * ACTIVE yolculuğu bitirir ve kesin ücret dökümünü döner (RentalController_finish). Araç
     * AVAILABLE olur; ödeme ayrı adımdır (POST /rentals/{id}/pay — bu iş kapsamında değil).
     */
    @POST("rentals/{id}/finish")
    suspend fun finish(@Path("id") rentalId: String): FinishRentalResponse

    /**
     * PREPARING (henüz başlamamış) kiralamayı iptal eder (RentalController_cancel, 204); araç
     * anında AVAILABLE olur. Foto ekranından başlatmadan çıkınca askıda kalan kaydı temizler.
     * ACTIVE yolculuk iptal edilemez (409). Gövdesiz yanıt olduğu için [Response] ile alınır.
     */
    @DELETE("rentals/{id}")
    suspend fun cancel(@Path("id") rentalId: String): Response<Unit>

    /**
     * Tek kiralamanın detayı (RentalController_getMine). Ödeme ekranı bitmiş yolculuğun ücret
     * dökümünü (totalPrice/startFee/serviceFee/durationMinutes/paymentStatus) buradan çeker.
     */
    @GET("rentals/{id}")
    suspend fun getRental(@Path("id") rentalId: String): RentalResponse

    /**
     * Tamamlanmış yolculuğu öder (RentalController_pay). method WALLET → cüzdandan düşer (yetersizse
     * 409); CARD → kayıtlı kartla simüle ödeme (cardId zorunlu). discountCode varsa indirim uygulanır.
     * Cevap ödeme makbuzudur (tutar dökümü + yöntem detayı).
     */
    @POST("rentals/{id}/pay")
    suspend fun pay(
        @Path("id") rentalId: String,
        @Body body: PayRentalRequest,
    ): PayRentalResponse
}
