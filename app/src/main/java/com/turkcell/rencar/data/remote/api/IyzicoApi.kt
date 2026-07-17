package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.CheckoutFormInitializeResponse
import com.turkcell.rencar.data.remote.dto.InitializeCheckoutFormRequest
import com.turkcell.rencar.data.remote.dto.IyzicoPaymentResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * İyzico Checkout Form uçları (openapi.json — tag: Iyzico). Auth zorunlu (AuthInterceptor Bearer
 * ekler), yalnızca CUSTOMER rolü erişebilir; 503 → sunucuda İyzico anahtarları ayarlanmamış.
 *
 * Yalnızca ortak ödeme sayfası (Checkout Form) akışının iki ucu tanımlıdır. Doğrudan kart ödemesi
 * (POST /iyzico/payments) ve 3DS başlatma (POST /iyzico/payments/threeds/initialize) uçları kart
 * bilgisini uygulamada toplamayı gerektirdiğinden KULLANILMAZ; iptal/iade uçları ADMIN'e aittir
 * (AGENTS §2.2 — kapsam dışı uçlar için istemci yazılmaz).
 */
interface IyzicoApi {

    /**
     * Ortak ödeme sayfasını başlatır (IyzicoController_initializeCheckoutForm). Kart bilgisi
     * İSTENMEZ; dönen `paymentPageUrl` WebView'da açılır. 400 → İyzico isteği reddetti.
     */
    @POST("iyzico/checkout-form/initialize")
    suspend fun initializeCheckoutForm(
        @Body body: InitializeCheckoutFormRequest,
    ): CheckoutFormInitializeResponse

    /**
     * Form oturumunun sonucunu İyzico'dan okur (IyzicoController_checkoutFormResult).
     * `paymentStatus=SUCCESS` tahsilatın tamamlandığı anlamına gelir. 400 → token geçersiz.
     */
    @GET("iyzico/checkout-form/result/{token}")
    suspend fun getCheckoutFormResult(@Path("token") token: String): IyzicoPaymentResponse
}
