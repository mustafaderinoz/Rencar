package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toVerificationStatus
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.remote.api.LicenseApi
import com.turkcell.rencar.data.image.toImagePart
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ehliyet yükleme iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → LicenseApi. Yüklemeden önce her görsel 5MB altına sıkıştırılır
 * (ortak [toImagePart]); hata yönetimi Result ile çağırana taşınır (mesaj eşlemesi ViewModel'de).
 */
@Singleton
class LicenseRepository @Inject constructor(
    private val licenseApi: LicenseApi,
) {
    /**
     * Ehliyet ön + arka yüzü ile canlılık selfie'sini yükler. [front]/[back]/[selfie] uygulama iç
     * depodaki JPEG dosyalarıdır; sıkıştırılmış kopya aynı dizinde "*-upload.jpg" olarak üretilip
     * multipart gönderilir. Selfie backend'de zorunludur (D5 — bkz. decisions.md).
     */
    suspend fun upload(front: File, back: File, selfie: File): Result<Unit> = runCatching {
        val frontPart = front.toImagePart(field = "front")
        val backPart = back.toImagePart(field = "back")
        val selfiePart = selfie.toImagePart(field = "selfie")
        licenseApi.upload(front = frontPart, back = backPart, selfie = selfiePart)
    }.map { }

    /** Mevcut kullanıcının ehliyet doğrulama durumunu getirir ([LicenseVerificationStatus]). */
    suspend fun getStatus(): Result<LicenseVerificationStatus> = runCatching {
        licenseApi.status().toVerificationStatus()
    }
}
