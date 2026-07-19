package com.turkcell.rencar.data.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

/**
 * Yüklemeden önce ehliyet fotoğraflarını küçültüp sıkıştırır.
 *
 * `POST /license/upload` dosya başına **maks. 5MB** kabul eder; kamera tam çözünürlük
 * bunu kolayca aşar. Bu yardımcı: (1) bellek-güvenli örnekleme ile decode eder,
 * (2) EXIF yönünü düzeltir, (3) en uzun kenarı [MAX_DIMENSION]'a indirir, (4) 5MB altına
 * inene kadar JPEG kalitesini düşürerek sıkıştırır. Android graphics API'leri saf dosya
 * yolu üzerinden çalıştığı için Context gerekmez.
 */
object ImageCompressor {

    private const val MAX_DIMENSION = 1600
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024
    private const val INITIAL_QUALITY = 85
    private const val MIN_QUALITY = 50

    /**
     * [source] görselini sıkıştırıp [target] dosyasına (JPEG) yazar ve [target]'ı döner.
     * [source] decode edilemezse [source] olduğu gibi geri döner (çağıran yine de yükleyebilsin).
     */
    fun compressForUpload(source: File, target: File): File {
        val bitmap = decodeSampled(source) ?: return source
        val scaled = scaleDown(bitmap)
        val rotated = applyExifRotation(scaled, source)

        var quality = INITIAL_QUALITY
        do {
            target.outputStream().use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            quality -= 10
        } while (target.length() > MAX_FILE_BYTES && quality >= MIN_QUALITY)

        if (rotated != bitmap) rotated.recycle()
        if (scaled != bitmap) scaled.recycle()
        bitmap.recycle()
        return target
    }

    /** En uzun kenar [MAX_DIMENSION]'ı aşmayacak kabaca örnekleme oranıyla decode eder. */
    private fun decodeSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / sample > MAX_DIMENSION * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeFile(file.absolutePath, opts)
    }

    /** En uzun kenar [MAX_DIMENSION]'dan büyükse orantılı küçültür; değilse aynısını döner. */
    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= MAX_DIMENSION) return bitmap
        val ratio = MAX_DIMENSION.toFloat() / longest
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    /** EXIF Orientation etiketine göre görüntüyü döndürür (kamera fotoğrafları yatık gelebilir). */
    private fun applyExifRotation(bitmap: Bitmap, source: File): Bitmap {
        val degrees = runCatching {
            when (ExifInterface(source.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
