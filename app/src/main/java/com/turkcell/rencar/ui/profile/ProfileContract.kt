package com.turkcell.rencar.ui.profile

import com.turkcell.rencar.data.model.LicenseVerificationStatus

/**
 * 07 Profil — saf UI durumu (§4.2).
 *
 * Alt navigasyon "Profil" sekmesi. Ad/telefon GET /auth/me'den, ehliyet doğrulama durumu
 * GET /license/status'tan gelir (dinamik). Menü satırları (ödeme/ayarlar/yardım/davet) ve
 * çıkış yalnızca görseldir (statik) — bu iş kapsamında bir aksiyona bağlanmaz (§4.6).
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
)

/**
 * Kullanıcı/ekran aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface ProfileIntent {
    /** Ekran açılışında profili + ehliyet durumunu yükler. */
    data object Load : ProfileIntent

    /** Hata sonrası yeniden dener. */
    data object Retry : ProfileIntent
}
