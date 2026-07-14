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
