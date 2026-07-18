package com.turkcell.rencar.ui.rentals

import com.turkcell.rencar.data.model.RentalHistoryItemUi
import com.turkcell.rencar.data.model.RentalStatsUi

/**
 * Kiralamalarım (Geçmiş sekmesi) — saf UI durumu (§4.2). Sekmeye her girişte GET /rentals (liste) +
 * GET /rentals/stats (başlık özeti) paralel yüklenir. Tüm alanlar varsayılan değerlidir; yalnızca saf
 * UI durumunu tutar. Navigasyon yoktur (Home sekmesi içi, kendi kendine yeten ekran; §4.5–4.6).
 */
data class RentalsUiState(
    /** İlk yükleme (henüz veri yokken) sürüyor → tam ekran spinner. */
    val isLoading: Boolean = true,
    /** İlk yükleme hatası (tam ekran hata + tekrar dene); yoksa null. Liste kritiktir. */
    val loadError: String? = null,

    /** Kiralama listesi (yeniden eskiye — API sırasıyla). */
    val rentals: List<RentalHistoryItemUi> = emptyList(),
    /** Aylık özet ("Bu ay 6 yolculuk · ₺612 harcama"); yüklenene/başarısız olana kadar null. */
    val stats: RentalStatsUi? = null,
) {
    /** Liste boş ve yükleme/başarısızlık yok → boş-durum kartı gösterilir. */
    val isEmpty: Boolean get() = rentals.isEmpty()
}

/**
 * Kullanıcı aksiyonları (§4.3). Parametresiz aksiyonlar `data object`.
 */
sealed interface RentalsIntent {
    /** Sekmeye giriş: listeyi + özeti yükler/tazeler (veri varken sessiz). */
    data object Load : RentalsIntent

    /** İlk yükleme hatası sonrası yeniden dener. */
    data object Retry : RentalsIntent
}
