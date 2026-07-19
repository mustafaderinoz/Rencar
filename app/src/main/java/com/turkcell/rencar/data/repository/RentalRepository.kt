package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toHistoryItem
import com.turkcell.rencar.data.mapper.toResumableUi
import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.ActiveRentalUi
import com.turkcell.rencar.data.model.RentalHistoryItemUi
import com.turkcell.rencar.data.model.RentalPhotosUi
import com.turkcell.rencar.data.model.RentalReceiptUi
import com.turkcell.rencar.data.model.RentalStatsUi
import com.turkcell.rencar.data.model.RentalUi
import com.turkcell.rencar.data.model.ResumableRentalUi
import com.turkcell.rencar.data.model.VehiclePoint
import com.turkcell.rencar.data.remote.api.RentalApi
import com.turkcell.rencar.data.remote.dto.CreateRentalRequest
import com.turkcell.rencar.data.remote.socket.RideLocationClient
import com.turkcell.rencar.data.image.toImagePart
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

/**
 * Kiralama + araç fotoğrafı iş akışı (karar: decisions.md → data + repository).
 * ViewModel → Repository → RentalApi. Fotoğraflar yüklemeden önce 5MB altına sıkıştırılır
 * (ortak [toImagePart]); hata yönetimi Result ile çağırana taşınır (mesaj eşlemesi ViewModel'de).
 */
@Singleton
class RentalRepository @Inject constructor(
    private val rentalApi: RentalApi,
    private val rideLocationClient: RideLocationClient,
) {
    /**
     * PREPARING kiralama açar. [plan] PER_MINUTE/HOURLY (foto adımı yalnız bu planlarda). Araçta
     * aktif rezervasyon yoksa veya araç kiralanabilir değilse API 409 döner; hata Result'a taşınır.
     */
    suspend fun createRental(vehicleId: String, plan: String): Result<RentalUi> =
        runCatching { rentalApi.create(CreateRentalRequest(vehicleId, plan)).toUi() }

    /**
     * Günlük (DAILY) kiralama açar. Bu planda foto adımı YOKTUR: API kaydı anında ACTIVE yapar ve
     * fiyatı baştan kilitler; çağıran doğrudan Aktif Yolculuk ekranına geçer.
     *
     * `endDate` yalnız bu planda zorunludur ve gün hesabı ona göre yapılır. Rezervasyon ekranı
     * günlük planda "1 gün" tahmini gösterip fiyatı 1440 dk üzerinden hesapladığından iade tarihi
     * **şu an + [DAILY_RENTAL_DAYS] gün** olarak gönderilir — böylece ekranda görünen tutar ile
     * faturalanan tutar birebir eşleşir. Bu ayrıntı (tarih biçimi + gün sayısı) burada kapsüllenir;
     * UI'ın bilmesi gereken bir şey değildir.
     */
    suspend fun createDailyRental(vehicleId: String): Result<RentalUi> = runCatching {
        rentalApi.create(
            CreateRentalRequest(
                vehicleId = vehicleId,
                plan = DAILY_PLAN,
                endDate = isoUtcFromNow(days = DAILY_RENTAL_DAYS),
            ),
        ).toUi()
    }

    /**
     * GET /rentals: giriş yapan müşterinin tüm kiralamaları (yeniden eskiye), Kiralamalarım kartlarına
     * dönüştürülmüş. Ağ/yetki hatası Result olarak çağırana taşınır (mesaj eşlemesi ViewModel'de).
     */
    suspend fun getMyRentals(): Result<List<RentalHistoryItemUi>> =
        runCatching { rentalApi.listMine().map { it.toHistoryItem() } }

    /**
     * GET /rentals/stats: bu ayın (varsayılan) yolculuk özeti — Kiralamalarım başlığını besler.
     * Ay parametresi gönderilmez; sunucu bu ayı kullanır.
     */
    suspend fun getMonthlyStats(): Result<RentalStatsUi> =
        runCatching { rentalApi.stats().toUi() }

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
     * GET /rentals/{id}/photos: PREPARING kiralamanın foto akışının anlık durumu (yüklü yönler +
     * sayaç). Uygulama yeniden açıldığında yarım kalan akışı devralmak için kullanılır; hata Result'a taşınır.
     */
    suspend fun getPhotos(rentalId: String): Result<RentalPhotosUi> =
        runCatching { rentalApi.getPhotos(rentalId).toUi() }

    /**
     * Kullanıcının açık (PREPARING) kiralamasını bulur — varsa foto ekranı yeni kiralama açmak yerine
     * akışı buradan devralır. Aynı anda en fazla bir aktif kiralama olabildiğinden ilk PREPARING kayıt
     * döndürülür; yoksa null. Liste alınamazsa hata Result olarak taşınır (çağıran null gibi ele alır).
     */
    suspend fun findPreparingRental(): Result<ResumableRentalUi?> =
        runCatching {
            rentalApi.listMine().firstOrNull { it.status.equals("PREPARING", ignoreCase = true) }
                ?.toResumableUi()
        }

    /**
     * Yeniden açılışta kurtarılacak devam eden kiralamayı bulur: önce ACTIVE (Aktif Yolculuk),
     * yoksa PREPARING (foto devralma). Tek listMine çağrısıyla ikisi de değerlendirilir; hiçbiri
     * yoksa null. Liste alınamazsa hata Result olarak taşınır (Splash kurtarmayı atlar, Home'a düşer).
     */
    suspend fun findResumableRental(): Result<ResumableRentalUi?> =
        runCatching {
            val rentals = rentalApi.listMine()
            val target = rentals.firstOrNull { it.status.equals("ACTIVE", ignoreCase = true) }
                ?: rentals.firstOrNull { it.status.equals("PREPARING", ignoreCase = true) }
            target?.toResumableUi()
        }

    /**
     * PREPARING yolculuğu ACTIVE yapar (4 foto sonrası). Fotoğraflar eksikse veya yolculuk zaten
     * başlamış/bitmişse API 409 döner; hata Result olarak çağırana taşınır.
     */
    suspend fun startRental(rentalId: String): Result<Unit> =
        runCatching { rentalApi.start(rentalId) }.map { }

    /**
     * GET /rentals/active: kullanıcının aktif yolculuğunun anlık durumu (Aktif Kiralama ekranı
     * bunu periyodik çeker). Aktif yolculuk yoksa API 404 döner; hata Result olarak taşınır.
     */
    suspend fun getActiveRental(): Result<ActiveRentalUi> =
        runCatching { rentalApi.getActiveRental().toUi() }

    /**
     * POST /rentals/{id}/finish: ACTIVE yolculuğu bitirir ve kesin ücret dökümünü döndürür.
     * Zaten bitmiş/başkasına ait vb. durumda API 4xx döner; hata Result olarak çağırana taşınır.
     */
    suspend fun finishRental(rentalId: String): Result<RentalReceiptUi> =
        runCatching { rentalApi.finish(rentalId).toUi() }

    /**
     * Aktif araçtaki canlı konum akışı (Socket.IO '/ws/locations' → 'my-vehicle'). Kütüphane
     * [RideLocationClient] ardında; UI yalnızca Flow<VehiclePoint> görür (io.socket'e bağımlı kalmaz).
     */
    fun vehiclePositionStream(): Flow<VehiclePoint> = rideLocationClient.vehiclePositionStream()

    /**
     * PREPARING kiralamayı iptal eder (foto ekranından başlatmadan çıkınca temizlik). Sunucu
     * 404/409 (zaten iptal/başlamış) dönse bile temizlik amaçlı sessizce başarı sayılır; yalnız
     * ağ/beklenmeyen hatalar Result.failure olur.
     */
    suspend fun cancelRental(rentalId: String): Result<Unit> = runCatching {
        rentalApi.cancel(rentalId)
        Unit
    }

    /**
     * Şu andan [days] gün sonrasını API'nin beklediği ISO-8601 UTC biçiminde döndürür
     * ("2026-07-15T10:00:00.000Z"). minSdk 24 + core library desugaring kapalı olduğundan java.time
     * yerine [SimpleDateFormat]/[Calendar] kullanılır ([com.turkcell.rencar.data.mapper] kalıbı).
     */
    private fun isoUtcFromNow(days: Int): String {
        val utc = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(utc).apply { add(Calendar.DAY_OF_YEAR, days) }
        val formatter = SimpleDateFormat(ISO_UTC_PATTERN, Locale.US).apply { timeZone = utc }
        return formatter.format(calendar.time)
    }

    private companion object {
        const val DAILY_PLAN = "DAILY"

        /** Günlük planda iade tarihi: rezervasyon ekranındaki "1 gün" tahminiyle birebir. */
        const val DAILY_RENTAL_DAYS = 1

        /** CreateRentalDto.endDate biçimi (openapi.json örneği: "2026-07-15T10:00:00.000Z"). */
        const val ISO_UTC_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    }
}
