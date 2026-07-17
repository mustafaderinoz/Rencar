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

    /**
     * 00 Açılış (session restore) — gerçek başlangıç ekranı. Saklı token'la oturumu geri yükleyip
     * kullanıcıyı doğru ekrana (Home/License/LicensePending) yönlendirir; token yoksa Onboarding'e,
     * oturum geçersizse Login'e düşer. Yönlendirme sonrası geri yığından temizlenir.
     */
    const val SPLASH = "splash"

    /** 01 Onboarding — token'sız kullanıcının karşılama/tanıtım ekranı. */
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

    /**
     * 05b Ehliyet bekleme — ehliyeti incelemede (UNDER_REVIEW) olan PENDING kullanıcı buraya
     * kilitlenir; onay gelene kadar uygulama kullanılamaz (geri tuşu ekranda yutulur).
     */
    const val LICENSE_PENDING = "license_pending"

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

    // 07 Rezervasyon onayı — araç detayındaki "Rezerve Et" ile açılır; rezerve edilecek aracın
    // id'sini path argümanı olarak taşır: "reservation/{vehicleId}".
    const val RESERVATION = "reservation"
    const val RESERVATION_ARG_VEHICLE_ID = "vehicleId"
    const val RESERVATION_ROUTE = "$RESERVATION/{$RESERVATION_ARG_VEHICLE_ID}"

    /** Belirli bir araç için somut rezervasyon rotasını üretir ([Uri.encode] ile güvenli kodlama). */
    fun reservationRoute(vehicleId: String): String = "$RESERVATION/${Uri.encode(vehicleId)}"

    // 08 Araç durumu (kiralama öncesi fotoğraf) — Dakikalık/Saatlik rezervasyon sonrası açılır.
    // Kiralanacak araç id'si + plan path argümanı taşır: "rental_photos/{vehicleId}/{plan}".
    // (RentalPhotosViewModel açılışta POST /rentals ile kiralamayı PREPARING oluşturur.)
    const val RENTAL_PHOTOS = "rental_photos"
    const val RENTAL_PHOTOS_ARG_VEHICLE_ID = "vehicleId"
    const val RENTAL_PHOTOS_ARG_PLAN = "plan"
    const val RENTAL_PHOTOS_ROUTE =
        "$RENTAL_PHOTOS/{$RENTAL_PHOTOS_ARG_VEHICLE_ID}/{$RENTAL_PHOTOS_ARG_PLAN}"

    /** Somut araç durumu rotasını üretir ([plan]: PER_MINUTE/HOURLY; [Uri.encode] ile kodlanır). */
    fun rentalPhotosRoute(vehicleId: String, plan: String): String =
        "$RENTAL_PHOTOS/${Uri.encode(vehicleId)}/${Uri.encode(plan)}"

    // 09 Aktif Yolculuk — "Kiralamayı Başlat" (POST /rentals/{id}/start) sonrası açılır. Aktif
    // kiralama id'sini path argümanı taşır: "active_rental/{rentalId}". Ekran GET /rentals/active
    // ile anlık durumu çeker; harita canlı konumu Socket.IO'dan alır.
    const val ACTIVE_RENTAL = "active_rental"
    const val ACTIVE_RENTAL_ARG_RENTAL_ID = "rentalId"
    const val ACTIVE_RENTAL_ROUTE = "$ACTIVE_RENTAL/{$ACTIVE_RENTAL_ARG_RENTAL_ID}"

    /** Somut aktif yolculuk rotasını üretir ([Uri.encode] ile güvenli kodlama). */
    fun activeRentalRoute(rentalId: String): String = "$ACTIVE_RENTAL/${Uri.encode(rentalId)}"

    // 10 Ödeme — "Kiralamayı Bitir" (POST /rentals/{id}/finish) sonrası açılır. Ödenecek kiralamanın
    // id'sini path argümanı taşır: "payment/{rentalId}". Ekran GET /rentals/{id} ile ücret dökümünü,
    // GET /cards ile kartları, GET /wallet ile bakiyeyi çeker; POST /rentals/{id}/pay ile öder.
    const val PAYMENT = "payment"
    const val PAYMENT_ARG_RENTAL_ID = "rentalId"
    const val PAYMENT_ROUTE = "$PAYMENT/{$PAYMENT_ARG_RENTAL_ID}"

    /** Somut ödeme rotasını üretir ([Uri.encode] ile güvenli kodlama). */
    fun paymentRoute(rentalId: String): String = "$PAYMENT/${Uri.encode(rentalId)}"

    // Home içindeki nested NavHost sekme rotaları (MainScreen). İçerikler şimdilik placeholder.
    const val MAP = "map"
    const val HISTORY = "history"
    const val WALLET = "wallet"
    const val PROFILE = "profile"
}
