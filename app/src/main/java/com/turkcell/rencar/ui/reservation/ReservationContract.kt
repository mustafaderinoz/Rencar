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
    /** Araç kartı verisi; null iken yükleniyor/hata/kurtarma gösterilir. */
    val vehicle: VehicleUi? = null,
    /**
     * Rezerve/kurtarma navigasyonu için araç kimliği (ViewModel'in path argümanı). Yeniden açılış
     * kurtarmasında araç REZERVE olduğundan GET /vehicles/{id} 404 döner ([vehicle] null kalır); foto
     * ekranına geçiş kimliği yine de buradan taşınır.
     */
    val vehicleId: String = "",
    /** Seçili kiralama planı (plan çipleri). Varsayılan: Dakikalık. */
    val selectedPlan: RentalPlan = RentalPlan.PER_MINUTE,
    /** Seçili plana ait fiyat önizleme (GET /vehicles/{id}/quote); yükleniyorken/hata null olur. */
    val quote: QuoteUi? = null,
    /** Plan değişiminde quote yeniden çekilirken true (fiyat satırında ince yükleniyor gösterimi). */
    val isQuoteLoading: Boolean = false,
    /** "Kullanım şartları" onay kutusu. */
    val termsAccepted: Boolean = false,
    /**
     * POST /reservations (ve günlük planda ardından POST /rentals) sürüyor — buton spinner. Günlük
     * planda iki çağrı tek bir kullanıcı işlemi olduğundan spinner zincir bitene kadar açık kalır.
     */
    val isReserving: Boolean = false,
    /**
     * Dakikalık/Saatlik: POST /reservations başarılı → foto ekranına geçiş sinyali (§4.6: Effect
     * yerine state bayrağı). Günlük planda KULLANILMAZ — orada geçiş [startedRentalId] ile olur.
     */
    val reserved: Boolean = false,
    /**
     * Günlük: POST /rentals başarılı → açılan kiralamanın id'si (aktif yolculuk ekranına geçiş
     * sinyali). Günlük planda foto adımı yoktur; API kaydı anında ACTIVE yapar.
     */
    val startedRentalId: String? = null,
    /**
     * Aktif rezervasyonun (15 dk ücretsiz tutma) kalan süresi (sn); null iken aktif rezervasyon yok ve
     * geri sayım gösterilmez. GET /reservations/active'ten alınıp 1 sn'lik yerel sayaçla azalır (Aktif
     * Yolculuk sayacındaki resync kalıbıyla aynı). 0'a ininde tekrar null olur (araç sunucuda boşa çıkar).
     */
    val reservationRemaining: Int? = null,
    /**
     * Kurtarma görünümünün araç etiketi ("Renault Clio · 34 RNC 022") — araç REZERVE olup 404 döndüğünde
     * ([vehicle] null) minimal kart bunu aktif rezervasyonun araç özetinden gösterir; aksi halde null.
     */
    val recoveryVehicleLabel: String? = null,
    /** Yükleme/rezervasyon hata mesajı (yoksa null). */
    val errorMessage: String? = null,
) {
    /** Araç kiralanabilir mi (yalnızca AVAILABLE). Butonun temel koşulu. */
    val isAvailable: Boolean get() = vehicle?.status == "AVAILABLE"

    /** Bu araç için aktif rezervasyon var mı (geri sayım gösterilir; buton "Devam Et" olur). */
    val hasActiveReservation: Boolean get() = reservationRemaining != null

    /**
     * Alt buton aktif mi. Aktif rezervasyon varsa (kurtarma/tekrar) yeni rezervasyon açılmaz, doğrudan
     * devam edilir — bu yüzden müsaitlik/şart koşulu aranmaz; aksi halde araç müsait + şartlar onaylı olmalı.
     */
    val canReserve: Boolean
        get() = if (hasActiveReservation) {
            !isReserving && !isLoading
        } else {
            isAvailable && termsAccepted && !isReserving && !isLoading
        }
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

    /** Üst baştaki geri butonu — navigasyon Screen katmanında ele alınır (§4.5/§4.6). */
    data object BackClicked : ReservationIntent

    /** Ekran [ReservationUiState.reserved] geçişini yaptı → bayrak tüketilir. */
    data object ReservedHandled : ReservationIntent

    /** Ekran [ReservationUiState.startedRentalId] geçişini yaptı → değer tüketilir. */
    data object RentalStartedHandled : ReservationIntent
}
