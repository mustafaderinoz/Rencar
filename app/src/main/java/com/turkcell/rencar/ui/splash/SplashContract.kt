package com.turkcell.rencar.ui.splash

/**
 * 00 Açılış (session restore) — saf UI durumu (§4.2).
 *
 * Uygulama açılışında saklı token'la oturum sessizce geri yüklenmeye çalışılır; çözülen ilk hedef
 * [destination] ile ekrana bildirilir (§4.6: Effect kanalı yerine state bayrağı; navigasyon ekran
 * katmanında ele alınır). [destination] yalnız bir kez tüketilir (bkz. [SplashIntent.DestinationHandled]).
 */
data class SplashUiState(
    val isLoading: Boolean = true,
    /** Oturum doğrulanamadı (ağ/bilinmeyen hata) — kullanıcı "Tekrar Dene" ile yeniden başlatır. */
    val isError: Boolean = false,
    /** Açılışta çözülen hedef; null iken henüz karar verilmemiştir. */
    val destination: SplashDestination? = null,
)

/**
 * Açılışta çözülen ilk hedef.
 * - [Onboarding]: saklı token yok (ilk açılış veya çıkış yapılmış).
 * - [Login]: token vardı ama oturum geçersiz (refresh de başarısız) → temizlendi, yeniden giriş gerek.
 * - [Home] / [LicenseUpload] / [LicensePending]: geçerli oturum; rol + ehliyet durumuna göre
 *   (OTP sonrası yönlendirmeyle aynı kural — bkz. SplashViewModel.resolveDestination).
 * - [ActiveRental] / [PreparingRental] / [ActiveReservation]: CUSTOMER'da devam eden akış kurtarma
 *   (yeniden açılış). Ekrana taşınacak kimlikler (rentalId / vehicleId + plan) case'te tutulur — bu
 *   yüzden enum değil sealed; NavHost route'u bu alanlardan üretir.
 */
sealed interface SplashDestination {
    data object Onboarding : SplashDestination
    data object Login : SplashDestination
    data object Home : SplashDestination
    data object LicenseUpload : SplashDestination
    data object LicensePending : SplashDestination

    /** Devam eden ACTIVE kiralama → Aktif Yolculuk ekranı. */
    data class ActiveRental(val rentalId: String) : SplashDestination

    /** Yarım kalan PREPARING kiralama → Foto ekranı (akış devralınır, GET /rentals/{id}/photos). */
    data class PreparingRental(val vehicleId: String, val plan: String) : SplashDestination

    /** Aktif rezervasyon → Rezervasyon ekranı (geri sayım + "Devam Et" kurtarma görünümü). */
    data class ActiveReservation(val vehicleId: String) : SplashDestination
}

/** Kullanıcı aksiyonları (§4.3). */
sealed interface SplashIntent {
    /** Ağ hatası sonrası oturum geri-yüklemeyi yeniden dener. */
    data object Retry : SplashIntent

    /** Ekran [SplashUiState.destination] geçişini yaptı → bayrak tüketilir. */
    data object DestinationHandled : SplashIntent
}
