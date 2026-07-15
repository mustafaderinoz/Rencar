package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.RentalUi
import com.turkcell.rencar.data.remote.api.RentalApi
import com.turkcell.rencar.data.remote.dto.CreateRentalRequest
import com.turkcell.rencar.data.util.ImageCompressor
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Kiralama + araç fotoğrafı iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → RentalApi. Fotoğraflar yüklemeden önce 5MB altına sıkıştırılır
 * ([ImageCompressor]); hata yönetimi Result ile çağırana taşınır (mesaj eşlemesi ViewModel'de).
 */
@Singleton
class RentalRepository @Inject constructor(
    private val rentalApi: RentalApi,
) {
    /**
     * PREPARING kiralama açar. [plan] PER_MINUTE/HOURLY (foto adımı yalnız bu planlarda). Araçta
     * aktif rezervasyon yoksa veya araç kiralanabilir değilse API 409 döner; hata Result'a taşınır.
     */
    suspend fun createRental(vehicleId: String, plan: String): Result<RentalUi> =
        runCatching { rentalApi.create(CreateRentalRequest(vehicleId, plan)).toUi() }

    /**
     * Bir yönün ([side] = FRONT/BACK/LEFT/RIGHT) fotoğrafını yükler. [file] uygulama iç depodaki
     * JPEG'dir; sıkıştırılmış kopya aynı dizinde "<side>-upload.jpg" olarak üretilip gönderilir.
     * Cevap akışın güncel durumudur (sayaç + kalan yönler).
     */
    suspend fun uploadPhoto(rentalId: String, side: String, file: File): Result<RentalPhotosUi> =
        runCatching {
            val sidePart = MultipartBody.Part.createFormData("side", side)
            val filePart = file.toImagePart(field = "file", uploadName = "$side-upload.jpg")
            rentalApi.uploadPhoto(rentalId, sidePart, filePart).toUi()
        }

    /**
     * PREPARING yolculuğu ACTIVE yapar (4 foto sonrası). Fotoğraflar eksikse veya yolculuk zaten
     * başlamış/bitmişse API 409 döner; hata Result olarak çağırana taşınır.
     */
    suspend fun startRental(rentalId: String): Result<Unit> =
        runCatching { rentalApi.start(rentalId) }.map { }

    /**
     * PREPARING kiralamayı iptal eder (foto ekranından başlatmadan çıkınca temizlik). Sunucu
     * 404/409 (zaten iptal/başlamış) dönse bile temizlik amaçlı sessizce başarı sayılır; yalnız
     * ağ/beklenmeyen hatalar Result.failure olur.
     */
    suspend fun cancelRental(rentalId: String): Result<Unit> = runCatching {
        rentalApi.cancel(rentalId)
        Unit
    }

    /** Dosyayı sıkıştırıp "field" adıyla JPEG multipart parçasına dönüştürür. */
    private fun File.toImagePart(field: String, uploadName: String): MultipartBody.Part {
        val compressed = ImageCompressor.compressForUpload(
            source = this,
            target = File(parentFile, uploadName),
        )
        val body = compressed.asRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(field, compressed.name, body)
    }
}
