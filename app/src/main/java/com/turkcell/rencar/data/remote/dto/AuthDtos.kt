package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Auth akışı DTO'ları — openapi.json şemalarıyla birebir.
 * Parolasız iki adımlı giriş: POST /auth/login → POST /auth/verify-otp.
 */

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
