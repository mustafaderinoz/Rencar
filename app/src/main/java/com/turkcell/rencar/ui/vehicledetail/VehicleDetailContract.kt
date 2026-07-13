package com.turkcell.rencar.ui.vehicledetail

import com.turkcell.rencar.data.remote.dto.VehicleResponse

/**
 * Araç Detay — saf UI durumu (§4.2).
 *
 * Haritada bir araca tıklanınca alt sayfa (bottom sheet) olarak açılır; veri GET /vehicles/{id}
 * ile [com.turkcell.rencar.data.repository.VehicleRepository] üzerinden çekilir (statik değil).
 * Uzaklık, kullanıcı konumu ile aracın konumundan gerçek hesaplanır ([distanceMeters]).
 * Yakıt/menzil/vites/koltuk/fiyat alanlarının tümü API'den ([vehicle]) gelir.
 */
data class VehicleDetailUiState(
    val isLoading: Boolean = false,
    /** GET /vehicles/{id} sonucu; null iken içerik yerine yükleniyor/hata gösterilir. */
    val vehicle: VehicleResponse? = null,
    /** Kullanıcı konumu ↔ araç konumu arası metre; konum yoksa null (uzaklık satırı gizlenir). */
    val distanceMeters: Float? = null,
    /** Yükleme başarısızsa gösterilecek mesaj (yoksa null). */
    val errorMessage: String? = null,
) {
    /** Araç kiralanabilir/rezerve edilebilir mi (yalnızca AVAILABLE). Buton durumlarını sürer. */
    val isAvailable: Boolean get() = vehicle?.status == "AVAILABLE"
}

/**
 * Kullanıcı/ekran aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface VehicleDetailIntent {
    /** Detayı yükle: araç id'si + (varsa) uzaklık için kullanıcı konumu. Ekran açılışında tetiklenir. */
    data class Load(
        val vehicleId: String,
        val userLatitude: Double?,
        val userLongitude: Double?,
    ) : VehicleDetailIntent

    /** Hata sonrası son [Load] parametreleriyle yeniden dener. */
    data object Retry : VehicleDetailIntent

    /** "Rezerve Et" (araç AVAILABLE iken aktif). Rezervasyon ucu (POST /reservations) bu iş kapsamında bağlanmaz. */
    data object ReserveClicked : VehicleDetailIntent

    /** "Kilidi Aç" (yalnız rezerve/kiralanmış araçta aktif; şimdilik pasif). Bağlanmaz. */
    data object UnlockClicked : VehicleDetailIntent
}
