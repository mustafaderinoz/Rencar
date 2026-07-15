package com.turkcell.rencar.data.remote.interceptor

import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.remote.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Herhangi bir Retrofit çağrısı 401 dönünce access token'ı [SessionManager] ile sessizce yeniler ve
 * isteği taze token'la BİR kez tekrar dener. [AuthInterceptor] başlığı ekler; bu sınıf yalnız
 * yenileme+tekrar mantığını taşır (OkHttp bu ikisini ayrı sözleşmelerle çağırır).
 *
 * Kurallar:
 *  - **Başlıksız istekler** (login/verify-otp gibi açık uçlar) atlanır: oradaki 401 gerçek bir kimlik
 *    hatasıdır, token tazeleme çözmez.
 *  - **Sonsuz döngü koruması:** aynı isteği en fazla bir kez tekrar deneriz ([responseCount] >= 2 → bırak).
 *  - Refresh çağrısı AYRI bir istemciden (RefreshApi, Authenticator'sız) gittiği için buraya hiç
 *    uğramaz; bu sınıf kendini tetikleyemez.
 *  - `runBlocking` güvenli: OkHttp `authenticate`'i ağ thread'inde çağırır (AuthInterceptor ile aynı kalıp).
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val tokenStore: TokenStore,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Yalnız Authorization taşıyan (yani token'lı) isteklerde yenileme denenir.
        val failedToken = response.request.header(HEADER_AUTHORIZATION)
            ?.removePrefix(BEARER_PREFIX)
            ?: return null

        if (responseCount(response) >= MAX_ATTEMPTS) return null

        val refreshed = runBlocking { sessionManager.refreshSession(failedToken) }
        if (!refreshed) return null

        val newToken = runBlocking { tokenStore.currentAccessToken() } ?: return null
        return response.request.newBuilder()
            .header(HEADER_AUTHORIZATION, BEARER_PREFIX + newToken)
            .build()
    }

    /** Bu yanıta kadar zincirdeki (prior) yanıt sayısı: 1 = ilk 401, 2 = tekrar denenmiş. */
    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        const val BEARER_PREFIX = "Bearer "
        const val MAX_ATTEMPTS = 2
    }
}
