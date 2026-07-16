package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.TopupRequest
import com.turkcell.rencar.data.remote.dto.WalletResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Cüzdan uçları (openapi.json — tag: Wallet). Auth zorunlu, yalnızca CUSTOMER rolü erişebilir.
 * Ödeme ekranı bakiyeyi, Cüzdan ekranı bakiye + son işlemleri okur ve bakiye yükler.
 */
interface WalletApi {

    /**
     * Giriş yapan müşterinin cüzdanı (WalletController_getMine). İlk erişimde 0 TL ile otomatik
     * oluşturulur. Ödeme ekranı [WalletResponse.balance]'ı, Cüzdan ekranı ayrıca [WalletResponse.transactions]'ı kullanır.
     */
    @GET("wallet")
    suspend fun getWallet(): WalletResponse

    /**
     * Cüzdana bakiye yükler (WalletController_topup, 10–5000 TL — simülasyon). Güncel cüzdanı
     * (bakiye + son işlemler) döner; 400 → tutar aralık dışı.
     */
    @POST("wallet/topup")
    suspend fun topup(@Body body: TopupRequest): WalletResponse
}
