package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.WalletResponse
import retrofit2.http.GET

/**
 * Cüzdan ucu (openapi.json — tag: Wallet). Auth zorunlu, yalnızca CUSTOMER rolü erişebilir.
 * Ödeme ekranı cüzdanla ödeme seçeneğinde bakiyeyi bu uçtan okur.
 */
interface WalletApi {

    /**
     * Giriş yapan müşterinin cüzdanı (WalletController_getMine). İlk erişimde 0 TL ile otomatik
     * oluşturulur. Ödeme ekranı yalnız [WalletResponse.balance]'ı kullanır.
     */
    @GET("wallet")
    suspend fun getWallet(): WalletResponse
}
