package com.turkcell.rencar.data.model

/**
 * Kiralama — UI/domain modeli (decisions.md → "Katman Derinliği" mapper katmanı). DTO: RentalResponse.
 *
 * Foto ekranı yalnızca kiralama kimliğini ve araç başlığını/plakasını kullanır; başlık ("Marka Model")
 * mapper katmanında ([com.turkcell.rencar.data.mapper.toUi]) oluşturulur.
 */
data class RentalUi(
    val id: String,
    val vehicleTitle: String,
    val vehiclePlate: String,
)

/**
 * Kiralamalarım (Geçmiş) listesindeki tek kart — UI/domain modeli. DTO: RentalResponse.
 *
 * Tüm etiketler ([dateLabel] "26 Haz 2026 · 14:32", [durationLabel] "24 dk", [distanceLabel] "12,4 km")
 * mapper katmanında hazır üretilir. [priceLabel] yalnız tutarı kilitlenmiş (tamamlanmış) yolculukta
 * doludur ("₺110,50"); tutarı olmayan (aktif/hazırlanıyor/iptal) kayıtlarda null olur ve yerine
 * [statusLabel] ("Aktif" vb.) gösterilir — böylece yeni açılan kiralamalar da listede görünür.
 */
data class RentalHistoryItemUi(
    val id: String,
    val vehicleTitle: String,
    val dateLabel: String,
    val durationLabel: String,
    val distanceLabel: String,
    val priceLabel: String?,
    val statusLabel: String?,
)

/**
 * Kiralamalarım başlığındaki aylık özet — UI/domain modeli. DTO: RentalStatsResponse.
 * [summaryLabel] "Bu ay 6 yolculuk · ₺612 harcama" olarak mapper'da hazır üretilir; [tripCount]
 * boş-durum mantığı için tutulur.
 */
data class RentalStatsUi(
    val tripCount: Int,
    val summaryLabel: String,
)
