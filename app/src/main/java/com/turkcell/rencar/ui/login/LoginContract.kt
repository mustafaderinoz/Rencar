package com.turkcell.rencar.ui.login

/**
 * 02 Login — saf UI durumu (§4.2).
 * Parolasız OTP akışının 1. adımı: kullanıcı telefon numarasını girer, "Kod Gönder"
 * ile POST /auth/login çağrılır (ülke kodu sabit +90; state yalnızca haneyi tutar).
 */
data class LoginUiState(
    val phone: String = "",
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface LoginIntent {
    data class PhoneChanged(val phone: String) : LoginIntent
    data object SendCodeClicked : LoginIntent
    data object BackClicked : LoginIntent
    data object RegisterClicked : LoginIntent
}
