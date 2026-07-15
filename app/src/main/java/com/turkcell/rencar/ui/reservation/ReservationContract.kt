package com.turkcell.rencar.ui.reservation

import com.turkcell.rencar.data.model.QuoteUi
import com.turkcell.rencar.data.model.VehicleUi

/**
 * Rezervasyon Onayı — saf UI durumu (§4.2).
 *
 * "Rezerve Et" (araç detay) ile açılır; iki uç besler:
 * - GET /vehicles/{id} → araç kartı (marka/model/plaka/vites/koltuk/yakıt + plan fiyatları).
 * - GET /vehicles/{id}/quote → seçili plana göre fiyat önizleme ("Başlangıç ücreti" + "Tahmini ücret").
 * "Rezervasyonu Tamamla" → POST /reservations (araç 15 dk ücretsiz tutulur).
 *
 * Alanlar yalnızca UI durumunu tutar; navigasyon/etki ekran katmanındadır (§4.5–4.6).
 */
data class ReservationUiState(
    /** GET /vehicles/{id} yükleniyor (ilk açılış). */
    val isLoading: Boolean = false,
    /** Araç kartı verisi; null iken yükleniyor/hata gösterilir. */
    val vehicle: VehicleUi? = null,
    /** Seçili kiralama planı (plan çipleri). Varsayılan: Dakikalık. */
    val selectedPlan: RentalPlan = RentalPlan.PER_MINUTE,
    /** Seçili plana ait fiyat önizleme (GET /vehicles/{id}/quote); yükleniyorken/hata null olur. */
    val quote: QuoteUi? = null,
    /** Plan değişiminde quote yeniden çekilirken true (fiyat satırında ince yükleniyor gösterimi). */
    val isQuoteLoading: Boolean = false,
    /** "Kullanım şartları" onay kutusu. */
    val termsAccepted: Boolean = false,
    /** POST /reservations sürüyor (buton spinner). */
    val isReserving: Boolean = false,
    /** POST /reservations başarılı → geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val reserved: Boolean = false,
    /** Yükleme/rezervasyon hata mesajı (yoksa null). */
    val errorMessage: String? = null,
) {
    /** Araç kiralanabilir mi (yalnızca AVAILABLE). Butonun temel koşulu. */
    val isAvailable: Boolean get() = vehicle?.status == "AVAILABLE"

    /** "Rezervasyonu Tamamla" aktif mi: araç müsait + şartlar onaylı + başka işlem sürmüyor. */
    val canReserve: Boolean
        get() = isAvailable && termsAccepted && !isReserving && !isLoading
}

/**
 * Kiralama planı — tasarımdaki Dakikalık/Saatlik/Günlük çipleri.
 * [apiPlan] quote ucundaki enum, [estimateMinutes] "Tahmini ücret" için varsayılan süre,
 * [estimateLabel] o satırın parantez etiketi (ör. "30 dk").
 */
enum class RentalPlan(
    val apiPlan: String,
    val label: String,
    val estimateMinutes: Int,
    val estimateLabel: String,
) {
    PER_MINUTE("PER_MINUTE", "Dakikalık", 30, "30 dk"),
    HOURLY("HOURLY", "Saatlik", 60, "1 sa"),
    DAILY("DAILY", "Günlük", 1440, "1 gün"),
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface ReservationIntent {
    /** Araç + quote yükle (ekran açılışında ViewModel init'te tetiklenir). */
    data object Load : ReservationIntent

    /** Hata sonrası yeniden dener. */
    data object Retry : ReservationIntent

    /** Plan çipi seçildi → quote yeniden çekilir. */
    data class PlanSelected(val plan: RentalPlan) : ReservationIntent

    /** Kullanım şartları onay kutusu değişti. */
    data object TermsToggled : ReservationIntent

    /** "Rezervasyonu Tamamla" → POST /reservations. */
    data object ReserveClicked : ReservationIntent
}
