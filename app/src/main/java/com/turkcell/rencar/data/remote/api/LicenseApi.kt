package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.LicenseResponse
import com.turkcell.rencar.data.remote.dto.LicenseStatusResponse
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/** Ehliyet uçları (openapi.json — tag: License). Base URL: BuildConfig.BASE_URL. */
interface LicenseApi {

    /**
     * Ehliyet ön + arka yüz fotoğrafları + yüz doğrulama selfie'sini yükler (multipart/form-data).
     * UploadLicenseDto alanları: "front", "back", "selfie" (üçü de zorunlu — D5, yeni başvurular).
     * Auth zorunlu (AuthInterceptor Bearer ekler).
     */
    @Multipart
    @POST("license/upload")
    suspend fun upload(
        @Part front: MultipartBody.Part,
        @Part back: MultipartBody.Part,
        @Part selfie: MultipartBody.Part,
    ): LicenseResponse

    /** Mevcut kullanıcının ehliyet durumunu döner. Auth zorunlu (AuthInterceptor Bearer ekler). */
    @GET("license/status")
    suspend fun status(): LicenseStatusResponse
}
