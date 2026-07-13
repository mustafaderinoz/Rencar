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
    /** POST /auth/verify-otp başarılı → geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val verified: Boolean = false,
    /** Doğrulama sonrası gidilecek hedef; yalnızca [verified] true iken anlamlıdır. */
    val destination: PostVerifyDestination = PostVerifyDestination.HOME,
)

/**
 * OTP doğrulaması sonrası kullanıcının yönlendirileceği hedef (rol + ehliyet durumuna göre).
 * - [HOME]: onaylı kullanıcı (CUSTOMER/ADMIN) veya ehliyeti APPROVED.
 * - [LICENSE_UPLOAD]: PENDING + ehliyet yüklenmemiş/reddedilmiş (NOT_SUBMITTED/REJECTED).
 * - [LICENSE_PENDING]: PENDING + ehliyet incelemede (UNDER_REVIEW) → engelleyici bekleme ekranı.
 */
enum class PostVerifyDestination { HOME, LICENSE_UPLOAD, LICENSE_PENDING }

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface OtpVerificationIntent {
    data class OtpCodeChanged(val code: String) : OtpVerificationIntent
    data object VerifyClicked : OtpVerificationIntent
    data object ResendCodeClicked : OtpVerificationIntent
    data object BackClicked : OtpVerificationIntent
}