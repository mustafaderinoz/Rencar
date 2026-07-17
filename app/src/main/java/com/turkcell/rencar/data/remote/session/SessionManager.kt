package com.turkcell.rencar.data.remote.session

import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.remote.api.RefreshApi
import com.turkcell.rencar.data.remote.dto.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Oturum/token yaşam döngüsünü tek yerden yöneten koordinatör.
 *
 * Asıl işi: süresi dolan access token'ı, kullanıcıyı tekrar login'e zorlamadan, eldeki refresh
 * token'la sessizce yenilemek (POST /auth/refresh — "rotation": yeni access + yeni refresh döner).
 * Ağ tarafında [com.turkcell.rencar.data.remote.interceptor.TokenAuthenticator] herhangi bir istek
 * 401 alınca buradaki [refreshSession]'ı çağırır; isteği taze token'la tekrar dener.
 *
 * - **Tek-uçuş (single-flight):** Aynı anda birden çok istek 401 alsa da yalnız BİR refresh ağ
 *   çağrısı yapılır; diğerleri [mutex]'i bekler ve token'ın çoktan yenilendiğini görüp beklerler.
 * - **Rotation:** Başarılı refresh'te access VE refresh token'ın İKİSİ de yeniden yazılır; yoksa bir
 *   sonraki refresh, artık geçersiz olan eski refresh token'la başarısız olurdu.
 * - **Sert logout:** Refresh de başarısızsa (refresh token da ölmüş) token'lar temizlenir ve
 *   [forcedLogout] bir olay yayınlar; UI bu olayı dinleyip kullanıcıyı login'e yönlendirir.
 */
@Singleton
class SessionManager @Inject constructor(
    private val refreshApi: RefreshApi,
    private val tokenStore: TokenStore,
) {
    private val mutex = Mutex()

    private val _forcedLogout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Otomatik kurtarılamayan oturum sonu (refresh başarısız). UI dinleyip login'e atmalı. */
    val forcedLogout: SharedFlow<Unit> = _forcedLogout.asSharedFlow()

    /**
     * Access token'ı yeniler. Tek-uçuş kilidiyle korunur.
     *
     * @param failedAccessToken 401 alan isteğin taşıdığı access token. Kilit alındığında saklı token
     *   bundan farklıysa başka bir çağrı çoktan yenilemiştir → ağ çağrısı yapmadan başarı döner.
     * @return yenileme başarılıysa (veya zaten başkası yenilediyse) true; refresh de başarısızsa false.
     */
    suspend fun refreshSession(failedAccessToken: String?): Boolean = mutex.withLock {
        val current = tokenStore.currentAccessToken()
        if (failedAccessToken != null && current != null && current != failedAccessToken) {
            return@withLock true
        }

        val refreshToken = tokenStore.currentRefreshToken()
        if (refreshToken.isNullOrBlank()) {
            forceLogout()
            return@withLock false
        }

        runCatching { refreshApi.refresh(RefreshTokenRequest(refreshToken)) }.fold(
            onSuccess = { auth ->
                tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
                true
            },
            onFailure = {
                forceLogout()
                false
            },
        )
    }

    /**
     * Kullanıcı isteğiyle YEREL oturumu kapatır: token'ları temizler ve oturum-sonu olayını yayınlar
     * (UI login'e döner). Sunucudaki refresh oturumlarının iptali AYRIDIR ve
     * [com.turkcell.rencar.data.repository.AuthRepository.logout] içinde (POST /auth/logout) yapılır.
     * Refresh-hatası kaynaklı sert logout ([refreshSession]) ile AYNI olayı ([forcedLogout]) yayınlar
     * — her iki durumda da NavHost kullanıcıyı login'e yönlendirir (tek navigasyon yolu).
     */
    suspend fun logout() = forceLogout()

    /** Token'ları temizler ve oturum-sonu olayını yayınlar (login'e yönlendirme UI'da yapılır). */
    private suspend fun forceLogout() {
        tokenStore.clear()
        _forcedLogout.tryEmit(Unit)
    }
}
