package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toVerificationStatus
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.remote.api.LicenseApi
import com.turkcell.rencar.data.util.ImageCompressor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Ehliyet yükleme iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → LicenseApi. Yüklemeden önce her görsel 5MB altına sıkıştırılır
 * ([ImageCompressor]); hata yönetimi Result ile çağırana taşınır (mesaj eşlemesi ViewModel'de).
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi,
) {
    /**
     * Ehliyet ön + arka yüzünü yükler. [front]/[back] uygulama iç depodaki JPEG dosyalarıdır;
     * sıkıştırılmış kopya aynı dizinde "*-upload.jpg" olarak üretilip multipart gönderilir.
     */
    suspend fun upload(front: File, back: File): Result<Unit> = runCatching {
        val frontPart = front.toImagePart(field = "front")
        val backPart = back.toImagePart(field = "back")
        licenseApi.upload(front = frontPart, back = backPart)
    }.map { }

    /** Mevcut kullanıcının ehliyet doğrulama durumunu getirir ([LicenseVerificationStatus]). */
    suspend fun getStatus(): Result<LicenseVerificationStatus> = runCatching {
        licenseApi.status().toVerificationStatus()
    }

    /** Dosyayı sıkıştırıp "field" adıyla JPEG multipart parçasına dönüştürür. */
    private fun File.toImagePart(field: String): MultipartBody.Part {
        val compressed = ImageCompressor.compressForUpload(
            source = this,
            target = File(parentFile, "$field-upload.jpg"),
        )
        val body = compressed.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(field, compressed.name, body)
    }
}
