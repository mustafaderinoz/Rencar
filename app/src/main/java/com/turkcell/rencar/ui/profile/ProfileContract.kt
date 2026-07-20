package com.turkcell.rencar.ui.profile

import com.turkcell.rencar.data.model.LicenseVerificationStatus

/**
 * 07 Profil — saf UI durumu (§4.2).
 *
 * Alt navigasyon "Profil" sekmesi. Ad/telefon GET /auth/me'den, ehliyet doğrulama durumu
 * GET /license/status'tan gelir (dinamik). Menü satırları (ödeme/ayarlar/yardım/davet) yalnızca
 * görseldir (statik, §4.6). "Çıkış yap" ise aksiyona bağlıdır: onay pop-up'ı → POST /auth/logout →
 * oturum kapatılır ve NavHost login'e döner (bkz. ProfileViewModel / AuthRepository.logout).
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    /** GET /auth/me → fullName. Boşken başlık iskeleti/loading gösterilir. */
    val fullName: String = "",
    /** GET /auth/me → phone, gösterim biçiminde ("+90 5XX XXX XX XX"); yoksa boş. */
    val phone: String = "",
    /** GET /license/status → ehliyet kartının başlığını/rozetini/rengini süren durum. */
    val licenseStatus: LicenseVerificationStatus = LicenseVerificationStatus.UNKNOWN,
    /** Profil (me) yüklenemezse gösterilecek mesaj (yoksa null). */
    val errorMessage: String? = null,
    /** "Çıkış yap" onay pop-up'ı açık mı (butona basılınca true, vazgeç/onayla ile false). */
    val showLogoutConfirm: Boolean = false,
    /** Çıkış isteği sürüyor mu (onay butonunda spinner + tekrar-basma koruması). */
    val isLoggingOut: Boolean = false,
    /**
     * Kayıtlı tema tercihi (ThemeStore): `null` = kullanıcı seçmedi, sistem teması geçerli.
     * Ekran, gösterilecek ikonu bu değer ile `isSystemInDarkTheme()`i birleştirerek çözer.
     */
    val darkTheme: Boolean? = null,
)

/**
 * Kullanıcı/ekran aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface ProfileIntent {
    /** Ekran açılışında profili + ehliyet durumunu yükler. */
    data object Load : ProfileIntent

    /** Hata sonrası yeniden dener. */
    data object Retry : ProfileIntent

    /** "Çıkış yap" butonuna basıldı → onay pop-up'ını açar. */
    data object LogoutClicked : ProfileIntent

    /** Onay pop-up'ında "Vazgeç" / dışına dokunma → pop-up'ı kapatır. */
    data object DismissLogout : ProfileIntent

    /** Onay pop-up'ında "Çıkış yap" → oturumu kapatır (POST /auth/logout → NavHost login'e döner). */
    data object ConfirmLogout : ProfileIntent

    /**
     * Başlıktaki gece/gündüz butonuna basıldı. Hedef değeri EKRAN hesaplar (o an geçerli temanın
     * tersi); böylece sistem teması sorgusu (`isSystemInDarkTheme()`) ViewModel'e sızmaz.
     */
    data class ThemeToggled(val dark: Boolean) : ProfileIntent
}
