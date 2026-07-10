package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.AuthResponse
import com.turkcell.rencar.data.remote.dto.LoginRequest
import com.turkcell.rencar.data.remote.dto.OtpRequiredResponse
import com.turkcell.rencar.data.remote.dto.UserDto
import com.turkcell.rencar.data.remote.dto.VerifyOtpRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** Auth uçları (openapi.json — tag: Auth). Base URL: BuildConfig.BASE_URL. */
interface AuthApi {

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
