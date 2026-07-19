package com.turkcell.rencar.ui.login

/**
 * 02 Login — saf UI durumu (§4.2).
 * Parolasız OTP akışının 1. adımı: kullanıcı telefon numarasını girer, "Kod Gönder"
 * ile POST /auth/login çağrılır (ülke kodu sabit +90; state yalnızca haneyi tutar).
 */
data class LoginUiState(
    val phone: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    /** POST /auth/login başarılı → OTP ekranına geçiş sinyali (§4.6: Effect yerine state bayrağı). */
    val codeSent: Boolean = false,
    /**
     * POST /auth/login → 401 ("bu numaraya kayıtlı kullanıcı yok") → Kayıt ekranına geçiş sinyali.
     * Backend kayıtsız numaraya OTP GÖNDERMEDİĞİNDEN kayıtsızlık ancak bu adımda anlaşılır; hata
     * mesajı göstermek yerine kullanıcı doğrudan kayda alınır (decisions.md → "Kayıt Ekranı").
     */
    val navigateToRegister: Boolean = false,
    /** Kayıt sonrası Login'e dönüldüğünde gösterilen başarı bilgisi (kullanıcı OTP ile giriş yapar). */
    val justRegistered: Boolean = false,
)

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface LoginIntent {
    data class PhoneChanged(val phone: String) : LoginIntent
    data object SendCodeClicked : LoginIntent
    data object BackClicked : LoginIntent
    data object RegisterClicked : LoginIntent

    /** Ekran [LoginUiState.codeSent] geçişini yaptı → bayrak tüketilir (tekrar geçişi önler). */
    data object CodeSentHandled : LoginIntent

    /** Ekran [LoginUiState.navigateToRegister] geçişini yaptı → bayrak tüketilir. */
    data object NavigateToRegisterHandled : LoginIntent
}
