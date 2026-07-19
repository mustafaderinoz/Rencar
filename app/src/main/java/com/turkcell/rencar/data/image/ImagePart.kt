package com.turkcell.rencar.data.image

import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Dosyayı 5MB altına sıkıştırıp ([ImageCompressor]) [field] adıyla JPEG multipart parçasına çevirir.
 * Sıkıştırılmış kopya aynı dizinde [uploadName] adıyla üretilir (varsayılan "<field>-upload.jpg").
 *
 * Ehliyet ve araç fotoğrafı yüklemelerinde ortak kullanılır (License/Rental repository) — tekrar önlenir.
 */
internal fun File.toImagePart(
    field: String,
    uploadName: String = "$field-upload.jpg",
): MultipartBody.Part {
    val compressed = ImageCompressor.compressForUpload(
        source = this,
        target = File(parentFile, uploadName),
    )
    val body = compressed.asRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData(field, compressed.name, body)
}
