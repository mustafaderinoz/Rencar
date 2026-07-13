package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.remote.api.AuthApi
import com.turkcell.rencar.data.remote.dto.AuthResponse
import com.turkcell.rencar.data.remote.dto.LoginRequest
import com.turkcell.rencar.data.remote.dto.OtpRequiredResponse
import com.turkcell.rencar.data.remote.dto.UserDto
import com.turkcell.rencar.data.remote.dto.VerifyOtpRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth iş akışı: parolasız iki adımlı giriş (karar: decisions.md → data + repository).
 * ViewModel → Repository → AuthApi. Hata yönetimi çağıran tarafa Result ile taşınır;
 * kullanıcıya gösterilecek mesaj eşlemesi (endpoint'e özgü) ViewModel'de yapılır.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    /** 1. adım: telefona SMS kodu gönder. phone E.164 biçiminde ("+90XXXXXXXXXX"). */
    suspend fun login(phone: String): Result<OtpRequiredResponse> = runCatching {
        authApi.login(LoginRequest(phone = phone))
    }

    /** 2. adım: kodu doğrula ve dönen token çiftini DataStore'a yaz. */
    suspend fun verifyOtp(phone: String, code: String): Result<AuthResponse> = runCatching {
        authApi.verifyOtp(VerifyOtpRequest(phone = phone, code = code)).also { auth ->
            tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
        }
    }

    /** Geçerli token sahibinin profili (GET /auth/me). Profil ekranı ad/telefon/rol için okur. */
    suspend fun me(): Result<UserDto> = runCatching {
        authApi.me()
    }
}
