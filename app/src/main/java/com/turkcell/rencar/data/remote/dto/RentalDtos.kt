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
 * POST /rentals gövdesi (CreateRentalDto). Dakikalık/Saatlik planda [endDate] gönderilmez
 * (yalnız DAILY'de zorunlu — bu akışta DAILY foto adımını atladığı için hiç kullanılmaz).
 */
@Serializable
data class CreateRentalRequest(
    val vehicleId: String,
    val plan: String,
)

/**
 * POST /rentals 201 yanıtı (RentalResponseDto). Foto ekranı yalnızca kiralama kimliğini ve
 * araç özetini (başlık: "Renault Clio · 34 RNC 022") kullanır; kalan alanlar okunmaz.
 */
@Serializable
data class RentalResponse(
    val id: String,
    val vehicleId: String,
    val vehicle: RentalVehicleSummary,
    val plan: String,
    val status: String,
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
