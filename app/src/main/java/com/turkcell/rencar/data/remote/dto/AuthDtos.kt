package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Auth akışı DTO'ları — openapi.json şemalarıyla birebir.
 * Parolasız iki adımlı giriş: POST /auth/login → POST /auth/verify-otp.
 * Kayıt: POST /auth/register (kullanıcı PENDING rolüyle oluşur).
 */

/**
 * POST /auth/register gövdesi (RegisterDto).
 *
 * [phone] E.164 ("+90" + 10 hane) ve benzersiz; [email] de benzersiz — ikisinin çakışması da
 * 409 döner (yalnız gövde metni ayırır, bkz. [com.turkcell.rencar.data.mapper.toRegisterError]).
 * [password] en az 6 karakter: kayıtta ZORUNLUdur ancak giriş parolasız OTP olduğundan sonradan
 * kullanılmaz (backend sözleşmesi böyle).
 * [referralCode] isteğe bağlı davet kodudur (profil "Davet et · ₺50 kazan" akışı); geçersizse 400
 * döner. Verilmediğinde `explicitNulls = false` (di/NetworkModule) sayesinde gövdeye HİÇ yazılmaz.
 */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String,
    val referralCode: String? = null,
)

/** POST /auth/login gövdesi (LoginDto). Telefon E.164: "+90" + 10 hane. */
@Serializable
data class LoginRequest(
    val phone: String,
)

/** POST /auth/login 200 yanıtı (OtpRequiredResponseDto). Token DÖNMEZ. */
@Serializable
data class OtpRequiredResponse(
    val message: String,
    val phone: String,
    val expiresAt: String,
)

/** POST /auth/verify-otp gövdesi (VerifyOtpDto). code: 6 haneli. */
@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val code: String,
)

/** POST /auth/refresh gövdesi (RefreshTokenDto). Geçerli refresh token'la yeni token çifti alınır. */
@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

/** /auth/verify-otp & /auth/register & /auth/refresh 200 yanıtı (AuthResponseDto). */
@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserDto,
)

/** Kullanıcı bilgisi (UserResponseDto). role: PENDING | CUSTOMER | ADMIN. */
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val phone: String? = null,
    val fullName: String,
    val role: String,
    val createdAt: String,
    val updatedAt: String,
)
