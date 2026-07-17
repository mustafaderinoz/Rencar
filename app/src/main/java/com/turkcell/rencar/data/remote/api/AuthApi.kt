package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.AuthResponse
import com.turkcell.rencar.data.remote.dto.LoginRequest
import com.turkcell.rencar.data.remote.dto.OtpRequiredResponse
import com.turkcell.rencar.data.remote.dto.RegisterRequest
import com.turkcell.rencar.data.remote.dto.UserDto
import com.turkcell.rencar.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Auth uçları (openapi.json — tag: Auth). Base URL: BuildConfig.BASE_URL. */
interface AuthApi {

    /**
     * Yeni kullanıcı kaydı; kullanıcı PENDING rolüyle oluşur.
     *
     * 201 ile [AuthResponse] (token çifti) döner — sözleşmeye sadık kalmak için tip korunur, fakat
     * token BİLİNÇLİ OLARAK KULLANILMAZ: kayıt sonrası Login'e dönülür (bkz. AuthRepository.register).
     * Hatalar: 409 e-posta VEYA telefon zaten kayıtlı · 400 alan doğrulama / davet kodu geçersiz.
     */
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    /** 1. adım: telefona SMS kodu "gönder" (simülasyon). Token dönmez. */
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): OtpRequiredResponse

    /** 2. adım: kodu doğrula, access + refresh token al. */
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequest): AuthResponse

    /** Geçerli token sahibinin profili + rolü. */
    @GET("auth/me")
    suspend fun me(): UserDto
}
