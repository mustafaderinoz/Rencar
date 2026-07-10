package com.turkcell.rencar.ui.license

/**
 * 05 Ehliyet Doğrulama (1. adım) — saf UI durumu (§4.2).
 * Kullanıcı ehliyetinin ön ve arka yüzünü kamerayla çeker; her ikisi de hazır olunca
 * "Devam Et" ile selfie adımına geçilir. Çekilen dosyaların yolları burada tutulur;
 * gerçek yükleme selfie sonrası (SelfieViewModel) yapılır.
 */
data class LicenseUiState(
    val frontPath: String? = null,
    val backPath: String? = null,
    /** İkisi de çekildiyse Devam Et etkinleşir. */
    val canContinue: Boolean = false,
    /** Devam Et → selfie ekranına geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val proceed: Boolean = false,
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface LicenseIntent {
    data class FrontCaptured(val path: String) : LicenseIntent
    data class BackCaptured(val path: String) : LicenseIntent
    data object ContinueClicked : LicenseIntent
    data object BackClicked : LicenseIntent
}
