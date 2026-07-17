package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.mapper.toRegisterError
import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.RegisterException
import com.turkcell.rencar.data.model.UserUi
import com.turkcell.rencar.data.remote.api.AuthApi
import com.turkcell.rencar.data.remote.dto.LoginRequest
import com.turkcell.rencar.data.remote.dto.RegisterRequest
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
    /**
     * Yeni kullanıcı kaydı (POST /auth/register); kullanıcı PENDING rolüyle oluşur.
     *
     * API 201 ile token çifti döndürür ANCAK BİLİNÇLİ OLARAK YOK SAYILIR: kayıt sonrası kullanıcı
     * Login'e yönlendirilip normal OTP akışıyla giriş yapar (kullanıcı kararı; decisions.md →
     * "Kayıt Ekranı"). Bu yüzden [tokenStore]'a HİÇBİR ŞEY YAZILMAZ ve yanıt Result<Unit>'e
     * indirgenir (login/reserve/upload ile aynı kalıp).
     *
     * Hata, alan-bazlı gösterim için tiplenmiş [RegisterException] olarak taşınır; ayrıştırma
     * mapper katmanındadır, ViewModel sunucunun hata şemasını görmez.
     *
     * @param phone E.164 ("+90XXXXXXXXXX")
     * @param referralCode isteğe bağlı; boş/yalnız boşluk ise gövdeye yazılmaz (explicitNulls=false)
     */
    suspend fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        referralCode: String?,
    ): Result<Unit> = runCatching {
        authApi.register(
            RegisterRequest(
                email = email,
                password = password,
                fullName = fullName,
                phone = phone,
                referralCode = referralCode?.takeIf(String::isNotBlank),
            ),
        )
    }.fold(
        onSuccess = { Result.success(Unit) },
        onFailure = { Result.failure(RegisterException(it.toRegisterError())) },
    )

    /** 1. adım: telefona SMS kodu gönder. phone E.164 biçiminde ("+90XXXXXXXXXX"). Yanıt kullanılmaz → Unit. */
    suspend fun login(phone: String): Result<Unit> = runCatching {
        authApi.login(LoginRequest(phone = phone))
    }.map { }

    /** 2. adım: kodu doğrula, token çiftini DataStore'a yaz, doğrulanan kullanıcıyı ([UserUi]) döndür. */
    suspend fun verifyOtp(phone: String, code: String): Result<UserUi> = runCatching {
        authApi.verifyOtp(VerifyOtpRequest(phone = phone, code = code)).also { auth ->
            tokenStore.saveTokens(auth.accessToken, auth.refreshToken)
        }.user.toUi()
    }

    /** Geçerli token sahibinin profili (GET /auth/me) → [UserUi]. Profil ekranı ad/telefon için okur. */
    suspend fun me(): Result<UserUi> = runCatching {
        authApi.me().toUi()
    }
}
