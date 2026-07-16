package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.WalletUi
import com.turkcell.rencar.data.remote.api.WalletApi
import com.turkcell.rencar.data.remote.dto.TopupRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cüzdan iş akışı (karar: decisions.md → data + repository, ayrı mapper katmanı). ViewModel →
 * WalletRepository → WalletApi. Kart yönetimi (list/add/default/delete) ayrı bir domain olduğundan
 * mevcut [PaymentRepository] üzerinden yeniden kullanılır (kod tekrarı yok — "Minimum Değişiklik").
 * Hata yönetimi Result ile çağırana taşınır; mesaj eşlemesi ViewModel'de.
 */
@Singleton
class WalletRepository @Inject constructor(
    private val walletApi: WalletApi,
) {
    /** GET /wallet: güncel bakiye + son 20 işlem. */
    suspend fun getWallet(): Result<WalletUi> =
        runCatching { walletApi.getWallet().toUi() }

    /** POST /wallet/topup: [amount] TL yükler (10–5000 — simülasyon); güncel cüzdanı döner. */
    suspend fun topup(amount: Double): Result<WalletUi> =
        runCatching { walletApi.topup(TopupRequest(amount)).toUi() }
}
