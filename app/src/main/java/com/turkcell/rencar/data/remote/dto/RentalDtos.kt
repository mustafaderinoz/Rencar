package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Kiralama akışı DTO'ları — openapi.json şemalarıyla birebir.
 *
 * Dakikalık/Saatlik planda kiralama önce PREPARING olarak açılır (araç kilitlenir ama süre
 * işlemez), 4 yön fotoğraflanır, sonra başlatılır. Fotoğraf ekranı iki ucu kullanır:
 * - POST /rentals            → PREPARING kiralama oluşturur (rentalId + araç özeti).
 * - POST /rentals/{id}/photos → bir yönün fotoğrafını yükler; güncel foto durumunu döner.
 */

/**
 * POST /rentals gövdesi (CreateRentalDto).
 *
 * [endDate] YALNIZ DAILY planda zorunludur (planlanan iade tarihi; gün hesabı buna göre yapılır ve
 * şu andan ileri olmalıdır). Dakikalık/Saatlik planda GÖNDERİLMEZ — verilirse API 400 döner;
 * `explicitNulls=false` sayesinde null iken gövdeye hiç yazılmaz (decisions.md → "Minimum Değişiklik",
 * additive + nullable-default). Biçim: ISO-8601 UTC, ör. "2026-07-15T10:00:00.000Z".
 */
@Serializable
data class CreateRentalRequest(
    val vehicleId: String,
    val plan: String,
    val endDate: String? = null,
)

/**
 * POST /rentals 201 + GET /rentals/{id} yanıtı (RentalResponseDto). Foto ekranı yalnızca kiralama
 * kimliğini ve araç özetini kullanır. Ödeme ekranı ise GET /rentals/{id} ile bitmiş kiralamanın
 * ücret dökümünü çeker: [totalPrice] toplam, [startFee] açılış, [serviceFee] hizmet bedeli
 * ("Kiralama ücreti" = totalPrice − startFee − serviceFee), [durationMinutes] süre etiketi ("(N dk)"),
 * [paymentStatus] UNPAID/PAID. Döküm alanları additive + nullable-default'tur (decisions.md
 * "Minimum Değişiklik") — create/start yanıtı bunları içermese de deserileştirme bozulmaz.
 */
@Serializable
data class RentalResponse(
    val id: String,
    val vehicleId: String,
    val vehicle: RentalVehicleSummary,
    val plan: String,
    val status: String,
    val totalPrice: Double? = null,
    val startFee: Double = 0.0,
    val serviceFee: Double? = null,
    val distanceKm: Double = 0.0,
    val durationMinutes: Double = 0.0,
    val paymentStatus: String? = null,
)

/** Kiralama yanıtındaki araç özeti (RentalVehicleSummaryDto). */
@Serializable
data class RentalVehicleSummary(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val type: String,
)

/**
 * POST /rentals/{id}/photos 200 yanıtı (RentalPhotosStateDto) — foto akışının anlık durumu.
 * [uploadedCount] "2/4 çekildi" sayacını, [remainingSides] "kalan foto" etiketini besler.
 * [photosComplete] 4 yön tamamlanınca true olur ("Kiralamayı Başlat" burada açılır).
 */
@Serializable
data class RentalPhotosState(
    val rentalId: String,
    val photos: List<RentalPhoto> = emptyList(),
    val uploadedCount: Int = 0,
    val remainingSides: List<String> = emptyList(),
    val photosComplete: Boolean = false,
)

/** Yüklenmiş tek yön fotoğrafı (RentalPhotoDto). side: FRONT | BACK | LEFT | RIGHT. */
@Serializable
data class RentalPhoto(
    val side: String,
    val imageUrl: String,
    val createdAt: String,
)

/**
 * GET /rentals/active 200 yanıtı (ActiveRentalResponseDto) — aktif yolculuğun ANLIK durumu.
 * Aktif Kiralama ekranı bunu periyodik çeker: [currentCost] "Anlık ücret", [distanceKm] "Mesafe",
 * [elapsedSeconds] geçen süre (yerel sayaç bununla resync edilir), [startFee] bilgi kartındaki
 * başlangıç ücreti. Kullanılmayan alanlar (userId, endedAt, serviceFee, discountAmount vb.)
 * okunmaz (Json.ignoreUnknownKeys). Sayısal alanlar "number" olduğundan Double alınır.
 */
@Serializable
data class ActiveRentalResponse(
    val id: String,
    val vehicle: RentalVehicleSummary,
    val plan: String,
    val startedAt: String,
    val startFee: Double = 0.0,
    val distanceKm: Double = 0.0,
    val elapsedSeconds: Double = 0.0,
    val currentCost: Double = 0.0,
    val status: String,
)

/**
 * POST /rentals/{id}/finish 200 yanıtı (FinishRentalResponseDto) — bitişteki ücret dökümü.
 * Aktif Kiralama ekranı bitiş özetinde kullanır: [usageFee] kullanım, [startFee] açılış,
 * [totalPrice] varsa toplam (null → usageFee+startFee'den türetilir). Ödeme bu adımda yapılmaz.
 */
@Serializable
data class FinishRentalResponse(
    val id: String,
    val vehicle: RentalVehicleSummary,
    val plan: String,
    val startFee: Double = 0.0,
    val usageFee: Double = 0.0,
    val distanceKm: Double = 0.0,
    val elapsedSeconds: Double = 0.0,
    val totalPrice: Double? = null,
    val status: String,
)
