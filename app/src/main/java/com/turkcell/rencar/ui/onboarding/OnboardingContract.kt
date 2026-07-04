package com.turkcell.rencar.ui.onboarding

/**
 * 01 Splash / Onboarding — saf UI durumu (§4.2).
 * Tasarımdaki 3 noktalı sayfa göstergesi için mevcut sayfa bilgisini tutar.
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val pageCount: Int = 3,
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface OnboardingIntent {
    data class PageChanged(val page: Int) : OnboardingIntent
    data object StartClicked : OnboardingIntent
    data object LoginClicked : OnboardingIntent
}
