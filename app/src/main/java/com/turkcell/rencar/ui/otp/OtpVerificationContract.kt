package com.turkcell.rencar.ui.otp

/**
 * 03 OTP Doğrulama — saf UI durumu (§4.2).
 * Parolasız OTP akışının 2. adımı: kullanıcı SMS ile gelen 6 haneli kodu girer
 * ve POST /auth/verify-otp çağrılır.
 */
data class OtpVerificationUiState(
    val phoneNumber: String = "",
    val otpCode: String = "",
    val timeRemaining: Int = 299, // Saniye cinsinden (4:59 — OTP 5 dk geçerli)
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** POST /auth/verify-otp başarılı → Home'a geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val verified: Boolean = false,
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface OtpVerificationIntent {
    data class OtpCodeChanged(val code: String) : OtpVerificationIntent
    data object VerifyClicked : OtpVerificationIntent
    data object ResendCodeClicked : OtpVerificationIntent
    data object BackClicked : OtpVerificationIntent
}