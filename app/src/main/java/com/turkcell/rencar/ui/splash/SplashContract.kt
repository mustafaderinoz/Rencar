package com.turkcell.rencar.ui.splash

/**
 * 00 Açılış (session restore) — saf UI durumu (§4.2).
 *
 * Uygulama açılışında saklı token'la oturum sessizce geri yüklenmeye çalışılır; çözülen ilk hedef
 * [destination] ile ekrana bildirilir (§4.6: Effect kanalı yerine state bayrağı; navigasyon ekran
 * katmanında ele alınır). [destination] yalnız bir kez tüketilir (bkz. SplashViewModel.onDestinationHandled).
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
 * - [ONBOARDING]: saklı token yok (ilk açılış veya çıkış yapılmış).
 * - [LOGIN]: token vardı ama oturum geçersiz (refresh de başarısız) → temizlendi, yeniden giriş gerek.
 * - [HOME] / [LICENSE_UPLOAD] / [LICENSE_PENDING]: geçerli oturum; rol + ehliyet durumuna göre
 *   (OTP sonrası yönlendirmeyle aynı kural — bkz. SplashViewModel.resolveDestination).
 */
enum class SplashDestination { ONBOARDING, LOGIN, HOME, LICENSE_UPLOAD, LICENSE_PENDING }

/** Kullanıcı aksiyonları (§4.3). */
sealed interface SplashIntent {
    /** Ağ hatası sonrası oturum geri-yüklemeyi yeniden dener. */
    data object Retry : SplashIntent
}
