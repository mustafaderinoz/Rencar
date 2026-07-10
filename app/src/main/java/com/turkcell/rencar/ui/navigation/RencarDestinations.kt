package com.turkcell.rencar.ui.navigation

import android.net.Uri

/**
 * Uygulamanın navigasyon rota (route) sabitleri.
 *
 * Mevcut ekranlar tek yönlü akışı oluşturur: Onboarding → Login → OTP. Her ekran tek
 * bir string route ile temsil edilir; yeni ekranlar (ör. register/home) eklendikçe
 * buraya genişletilir (§2.2: var olmayan ekran uydurulmaz).
 */
object RencarDestinations {

    /** 01 Splash / Onboarding — başlangıç ekranı. */
    const val ONBOARDING = "onboarding"

    /** 02 Login — parolasız OTP akışının 1. adımı. */
    const val LOGIN = "login"

    // 03 OTP doğrulama ekranı, doğrulanacak numarayı path argümanı olarak taşır: "otp/{phone}".
    const val OTP = "otp"
    const val OTP_ARG_PHONE = "phone"
    const val OTP_ROUTE = "$OTP/{$OTP_ARG_PHONE}"

    /** Belirli bir numara için somut OTP rotasını üretir ([Uri.encode] ile güvenli kodlama). */
    fun otpRoute(phone: String): String = "$OTP/${Uri.encode(phone)}"

    /** 05 Ehliyet doğrulama (1. adım) — PENDING kullanıcı OTP sonrası buraya yönlenir. */
    const val LICENSE = "license"

    // 06 Selfie doğrulama (2. adım): çekilen ehliyet ön/arka yollarını path argümanı taşır.
    const val SELFIE = "selfie"
    const val SELFIE_ARG_FRONT = "frontPath"
    const val SELFIE_ARG_BACK = "backPath"
    const val SELFIE_ROUTE = "$SELFIE/{$SELFIE_ARG_FRONT}/{$SELFIE_ARG_BACK}"

    /** Somut selfie rotasını üretir (dosya yolları [Uri.encode] ile güvenli kodlanır). */
    fun selfieRoute(frontPath: String, backPath: String): String =
        "$SELFIE/${Uri.encode(frontPath)}/${Uri.encode(backPath)}"

    /** 04 Home — alt navigasyonlu (Harita/Geçmiş/Cüzdan/Profil) ana kabuk. */
    const val HOME = "home"

    // Home içindeki nested NavHost sekme rotaları (MainScreen). İçerikler şimdilik placeholder.
    const val MAP = "map"
    const val HISTORY = "history"
    const val WALLET = "wallet"
    const val PROFILE = "profile"
}
