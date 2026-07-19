package com.turkcell.rencar.ui.onboarding

/**
 * 01 Splash / Onboarding — saf UI durumu (§4.2).
 * Tek statik tanıtım ekranıdır; tasarımdaki sayfa göstergesi için yalnız nokta SAYISINI tutar.
 * Sayfalar arası gezinme/pager YOKTUR — bu yüzden "aktif sayfa" durumu tutulmaz (gösterge hep ilk
 * noktayı vurgular).
 */
data class OnboardingUiState(
    val pageCount: Int = 3,
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface OnboardingIntent {
    data object StartClicked : OnboardingIntent
    data object LoginClicked : OnboardingIntent
}
