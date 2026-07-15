package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.AuthResponse
import com.turkcell.rencar.data.remote.dto.RefreshTokenRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Yalnız token yenileme ucu (openapi.json — POST /auth/refresh, "rotation").
 *
 * [AuthApi]'den AYRI tutulur ve DI'da **Authenticator/AuthInterceptor İÇERMEYEN** sade bir
 * OkHttp istemcisinden üretilir (bkz. di/NetworkModule). Böylece:
 *  - `SessionManager → RefreshApi → (sade istemci)` grafiği asiklik olur; ana istemcinin
 *    Authenticator'ının SessionManager'a bağımlılığıyla dairesel bağımlılık oluşmaz.
 *  - Refresh çağrısı 401 dönerse Authenticator'a UĞRAMAZ; kendini sonsuz tetikleyemez.
 */
interface RefreshApi {

    /** Geçerli refresh token'la yeni access + refresh çiftini al (refresh de yenilenir → rotation). */
    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenRequest): AuthResponse
}
