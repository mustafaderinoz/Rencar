package com.turkcell.rencar.util

/**
 * [AppError] → kullanıcıya gösterilebilir Türkçe metin çevirisinin TEK adresi.
 *
 * Hiçbir ViewModel hata metni üretmez; hepsi buradan çözülür. Mesaj tablosunun kaynağı
 * `docs/api/openapi.json` uç yanıtlarıdır — yeni bir uç eklendiğinde YALNIZCA bu dosya güncellenir,
 * ViewModel'lere dokunulmaz.
 *
 * Aynı HTTP kodu (400/403/404/409) ekrana göre farklı anlam taşıdığından çeviri bir [ErrorContext]
 * alır. Örn. 403:
 *  - [ErrorContext.MAP]          → "Araçları görmek için ehliyet onayınız gerekli."
 *  - [ErrorContext.ACTIVE_RENTAL_FINISH] → "Bu yolculuk size ait değil."
 *  - bağlam verilmezse           → genel yedek.
 */
enum class ErrorContext {
    /** POST /auth/login — kod gönderimi. (401 buraya DÜŞMEZ: kayıt akışına yönlendirme sinyalidir.) */
    LOGIN,

    /** POST /auth/verify-otp — kod doğrulama. */
    OTP,

    /** GET /vehicles — harita araç listesi. */
    MAP,

    /** GET /vehicles/{id} — araç detayı. */
    VEHICLE_DETAIL,

    /** Rezervasyon ekranı açılışı (araç + fiyat teklifi). */
    RESERVATION_LOAD,

    /** POST /reservations — rezervasyon oluşturma. */
    RESERVATION_CREATE,

    /** Rezervasyon ekranından doğrudan kiralamaya geçiş. */
    RESERVATION_RENT,

    /** POST /rentals — fotoğraf ekranında kiralama kaydı oluşturma. */
    RENTAL_CREATE,

    /** POST /rentals/{id}/start — yolculuğu başlatma. */
    RIDE_START,

    /** POST /rentals/{id}/photos — kiralama fotoğrafı yükleme. */
    RENTAL_PHOTO_UPLOAD,

    /** GET /rentals — "Kiralamalarım" listesi. */
    RENTALS_LIST,

    /** GET /rentals/active — aktif yolculuk durumu (poll). */
    ACTIVE_RENTAL_STATUS,

    /** POST /rentals/{id}/finish — yolculuğu bitirme. */
    ACTIVE_RENTAL_FINISH,

    /** Ödeme ekranı açılışı (döküm + kart + bakiye). */
    PAYMENT_LOAD,

    /** POST /rentals/{id}/pay — indirim kodu OLMADAN. */
    PAYMENT_PAY,

    /**
     * POST /rentals/{id}/pay — indirim kodu İLE. API'de kod doğrulama ucu olmadığından geçersiz kod
     * ancak ödeme anında anlaşılır; 404/409 kod-özel metne çözülür.
     */
    PAYMENT_PAY_DISCOUNT,

    /** İyzico checkout başlatma. */
    IYZICO_INIT,

    /**
     * İyzico'da tahsilat BAŞARILI olduktan sonraki `POST /rentals/{id}/pay`. Para çekilmiş
     * olduğundan metinler bunu gizlemez (ve "oturum bulunamadı" genel eşlemesi UYGULANMAZ).
     */
    IYZICO_SETTLE,

    /** POST /cards — kart kaydetme. */
    CARD_ADD,

    /** DELETE /cards/{id} — kart silme. */
    CARD_DELETE,

    /** Cüzdan ekranındaki kart aksiyonları (varsayılan yap / sil). */
    WALLET_CARD_ACTION,

    /** GET /wallet — bakiye + işlemler. */
    WALLET_LOAD,

    /** POST /wallet/topup — bakiye yükleme. */
    WALLET_TOPUP,

    /** GET /auth/me — profil. */
    PROFILE,

    /** POST /license/upload — ehliyet + selfie yükleme. */
    SELFIE_UPLOAD,

    /** AI araç önerisi. */
    AI_RECOMMENDATION,

    /** Bağlam belirtilmediğinde kullanılan genel yedek. */
    GENERIC,
}

/** [AppError]'ı ekran bağlamına göre kullanıcı metnine çevirir. */
fun AppError.toUserMessage(context: ErrorContext = ErrorContext.GENERIC): String = when (this) {
    AppError.Network -> resolveNetworkMessage(context)
    is AppError.Api -> resolveApiMessage(code, context)
    is AppError.Unknown -> resolveUnknownMessage(context)
}

/**
 * SIRA ÖNEMLİDİR (ilk eşleşen kazanır):
 *  1. bağlama özgü net eşlemeler (aynı kodun ekrana göre farklı anlamı),
 *  2. tüm ekranlarda ortak eşlemeler (401 = oturum yok),
 *  3. bağlamın genel yedeği (HTTP kodu metinde korunur).
 *
 * 5xx için ayrı bir dal YOKTUR: sunucu hataları bağlamın kendi yedeğine düşer, böylece kullanıcı
 * hangi işlemin başarısız olduğunu görür. Tek istisna [ErrorContext.IYZICO_INIT] 503'tür ve o da
 * (1) numaralı adımda ele alınır.
 */
private fun resolveApiMessage(code: Int, context: ErrorContext): String =
    contextualMessage(code, context)
        ?: sharedMessage(code, context)
        ?: fallbackMessage(code, context)

/** 1. adım — bağlama özgü net eşlemeler. */
private fun contextualMessage(code: Int, context: ErrorContext): String? = when (context) {
    ErrorContext.OTP -> when (code) {
        401 -> "Kod geçersiz veya süresi dolmuş."
        else -> null
    }

    ErrorContext.MAP -> when (code) {
        403 -> "Araçları görmek için ehliyet onayınız gerekli."
        else -> null
    }

    ErrorContext.VEHICLE_DETAIL -> when (code) {
        403 -> "Araç detayını görmek için ehliyet onayınız gerekli."
        404 -> "Bu araç şu anda müsait değil."
        else -> null
    }

    ErrorContext.RESERVATION_LOAD -> when (code) {
        403 -> "Rezervasyon için ehliyet onayınız gerekli."
        404 -> "Bu araç şu anda müsait değil."
        else -> null
    }

    ErrorContext.RESERVATION_CREATE -> when (code) {
        403 -> "Rezervasyon için ehliyet onayınız gerekli."
        404 -> "Araç bulunamadı."
        409 -> "Bu araç artık müsait değil veya zaten aktif bir rezervasyonunuz var."
        else -> null
    }

    ErrorContext.RESERVATION_RENT -> when (code) {
        400 -> "Kiralama başlatılamadı: iade tarihi geçersiz."
        403 -> "Kiralama için ehliyet onayınız gerekli."
        404 -> "Araç bulunamadı."
        409 -> "Zaten aktif bir kiralamanız var veya araç kiralanabilir değil."
        else -> null
    }

    ErrorContext.RENTAL_CREATE -> when (code) {
        403 -> "Kiralama için ehliyet onayınız gerekli."
        404 -> "Araç bulunamadı."
        409 -> "Kiralama başlatılamadı: rezervasyonunuz bulunamadı veya araç müsait değil."
        else -> null
    }

    ErrorContext.RIDE_START -> when (code) {
        403 -> "Bu kiralama size ait değil."
        404 -> "Kiralama bulunamadı."
        409 -> "Yolculuk başlatılamadı: fotoğraflar eksik veya yolculuk zaten başlamış."
        else -> null
    }

    ErrorContext.RENTAL_PHOTO_UPLOAD -> when (code) {
        400 -> "Fotoğraf geçersiz. Lütfen tekrar çekin."
        403 -> "Bu kiralama size ait değil."
        404 -> "Kiralama bulunamadı."
        409 -> "Yolculuk zaten başlamış; fotoğraf eklenemiyor."
        413 -> "Fotoğraf çok büyük (maks. 5MB)."
        else -> null
    }

    ErrorContext.RENTALS_LIST -> when (code) {
        403 -> "Kiralamalara erişim için hesabınızın onaylı olması gerekir."
        else -> null
    }

    ErrorContext.ACTIVE_RENTAL_STATUS -> when (code) {
        404 -> "Aktif yolculuk bulunamadı."
        else -> null
    }

    ErrorContext.ACTIVE_RENTAL_FINISH -> when (code) {
        403 -> "Bu yolculuk size ait değil."
        404 -> "Yolculuk bulunamadı."
        409 -> "Yolculuk bitirilemedi: zaten bitmiş olabilir."
        else -> null
    }

    ErrorContext.PAYMENT_LOAD -> when (code) {
        404 -> "Ödenecek yolculuk bulunamadı."
        else -> null
    }

    ErrorContext.PAYMENT_PAY -> when (code) {
        400 -> "Ödeme bilgileri geçersiz."
        403 -> "Bu yolculuk size ait değil."
        404 -> "Yolculuk veya kart bulunamadı."
        409 -> "Ödeme alınamadı: cüzdan bakiyesi yetersiz olabilir veya yolculuk zaten ödenmiş."
        else -> null
    }

    ErrorContext.PAYMENT_PAY_DISCOUNT -> when (code) {
        400 -> "Ödeme bilgileri geçersiz."
        403 -> "Bu yolculuk size ait değil."
        404 -> "Girdiğiniz indirim kodu bulunamadı. Kodu kontrol edin ya da kaldırıp tekrar deneyin."
        409 -> "İndirim kodu kullanılamıyor (limit dolmuş veya daha önce kullanılmış) ya da bakiye " +
            "yetersiz. Kodu kaldırıp tekrar deneyebilirsiniz."
        else -> null
    }

    ErrorContext.IYZICO_INIT -> when (code) {
        400 -> "İyzico ödeme isteğini reddetti. Lütfen tekrar deneyin."
        403 -> "Bu işlem için yetkiniz yok."
        503 -> "Ödeme sağlayıcı şu anda kullanılamıyor. Cüzdan veya kartla ödeyebilirsiniz."
        else -> null
    }

    ErrorContext.IYZICO_SETTLE -> when (code) {
        409 -> "Bu yolculuğun ödemesi zaten alınmış görünüyor."
        else -> null
    }

    ErrorContext.CARD_ADD -> when (code) {
        400 -> "Kart bilgileri geçersiz (son kullanma tarihi geçmiş olabilir)."
        else -> null
    }

    ErrorContext.CARD_DELETE, ErrorContext.WALLET_CARD_ACTION -> when (code) {
        404 -> "Kart bulunamadı."
        else -> null
    }

    ErrorContext.WALLET_LOAD -> when (code) {
        403 -> "Cüzdana erişim için hesabınızın onaylı olması gerekir."
        else -> null
    }

    ErrorContext.WALLET_TOPUP -> when (code) {
        400 -> "Tutar 10 – 5.000 ₺ aralığında olmalı."
        else -> null
    }

    ErrorContext.SELFIE_UPLOAD -> when (code) {
        400 -> "Fotoğraf geçersiz. Ehliyet adımına dönüp tekrar çekin."
        401 -> "Oturum doğrulanamadı. Lütfen tekrar giriş yapın."
        409 -> "Ehliyetiniz zaten incelemede veya onaylı."
        413 -> "Fotoğraf boyutu çok büyük. Lütfen tekrar deneyin."
        else -> null
    }

    ErrorContext.LOGIN,
    ErrorContext.PROFILE,
    ErrorContext.AI_RECOMMENDATION,
    ErrorContext.GENERIC,
    -> null
}

/**
 * 2. adım — ekranlar arası ortak eşleme. 401 hemen her uçta "token yok/geçersiz" demektir.
 *
 * [CONTEXTS_WITHOUT_SHARED_AUTH] bunun DIŞINDA kalır: LOGIN'de 401 kayıt yönlendirmesidir ve
 * IYZICO_SETTLE'da para çekilmiş olduğundan mesaj oturumu değil tahsilatı anlatmalıdır.
 */
private fun sharedMessage(code: Int, context: ErrorContext): String? = when {
    code == 401 && context !in CONTEXTS_WITHOUT_SHARED_AUTH ->
        "Oturum bulunamadı. Lütfen tekrar giriş yapın."
    else -> null
}

private val CONTEXTS_WITHOUT_SHARED_AUTH = setOf(
    ErrorContext.LOGIN,
    ErrorContext.IYZICO_SETTLE,
)

/** 3. adım — bağlamın genel yedeği; HTTP kodu teşhis için metinde korunur. */
private fun fallbackMessage(code: Int, context: ErrorContext): String = when (context) {
    ErrorContext.MAP -> "Araçlar yüklenemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.VEHICLE_DETAIL,
    ErrorContext.RESERVATION_LOAD,
    -> "Araç yüklenemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.RESERVATION_CREATE -> "Rezervasyon oluşturulamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.RESERVATION_RENT -> "Kiralama başlatılamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.RENTAL_CREATE -> "Kiralama oluşturulamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.RIDE_START -> "Yolculuk başlatılamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.RENTAL_PHOTO_UPLOAD -> "Fotoğraf yüklenemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.RENTALS_LIST -> "Kiralamalar alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.ACTIVE_RENTAL_STATUS -> "Yolculuk durumu alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.ACTIVE_RENTAL_FINISH -> "Yolculuk bitirilemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.PAYMENT_LOAD -> "Ödeme bilgileri alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.PAYMENT_PAY,
    ErrorContext.PAYMENT_PAY_DISCOUNT,
    -> "Ödeme alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.IYZICO_INIT -> "İyzico ödemesi başlatılamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.IYZICO_SETTLE -> "Ödemeniz alındı ancak yolculuğa işlenemedi ($code). " +
        "Lütfen tekrar deneyin; tutar iki kez tahsil edilmez."
    ErrorContext.CARD_ADD -> "Kart kaydedilemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.CARD_DELETE -> "Kart silinemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.WALLET_CARD_ACTION -> "İşlem tamamlanamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.WALLET_LOAD -> "Cüzdan bilgileri alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.WALLET_TOPUP -> "Bakiye yüklenemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.PROFILE -> "Profil yüklenemedi ($code). Lütfen tekrar deneyin."
    ErrorContext.AI_RECOMMENDATION -> "Öneri alınamadı ($code). Lütfen tekrar deneyin."
    ErrorContext.LOGIN,
    ErrorContext.OTP,
    ErrorContext.SELFIE_UPLOAD,
    ErrorContext.GENERIC,
    -> "Bir hata oluştu ($code). Lütfen tekrar deneyin."
}

/** Ağa ulaşılamadı. Tek istisna: İyzico tahsilatı sonrası kopan bağlantı (para çekilmiştir). */
private fun resolveNetworkMessage(context: ErrorContext): String = when (context) {
    ErrorContext.IYZICO_SETTLE ->
        "Ödemeniz alındı ancak bağlantı koptuğu için işlenemedi. Lütfen tekrar deneyin."
    ErrorContext.AI_RECOMMENDATION -> "Öneri alınamadı: İnternet bağlantısı kurulamadı."
    else -> "İnternet bağlantısı kurulamadı."
}

/** Sınıflandırılamayan hata. Ham hata mesajı KULLANICIYA GÖSTERİLMEZ. */
private fun resolveUnknownMessage(context: ErrorContext): String = when (context) {
    ErrorContext.IYZICO_SETTLE ->
        "Ödemeniz alındı ancak yolculuğa işlenemedi. Lütfen tekrar deneyin."
    ErrorContext.AI_RECOMMENDATION -> "Öneri alınamadı. Lütfen tekrar deneyin."
    else -> "Beklenmeyen bir hata oluştu."
}

/**
 * Ağ hatası OLMAYAN, kullanıcı girdisine/akışa bağlı metinler. Bunlar da ViewModel'de değil
 * burada durur; ViewModel yalnızca hangi ALANA yazılacağına karar verir.
 */
object FormMessages {

    // --- Kayıt (yerel doğrulama) ---
    const val FULL_NAME_BLANK = "Ad soyad boş olamaz."
    const val INVALID_EMAIL = "Geçerli bir e-posta adresi gir."
    const val INVALID_PHONE = "Geçerli bir telefon numarası gir."
    fun passwordTooShort(minLength: Int) = "Şifre en az $minLength karakter olmalı."

    // --- Kayıt (sunucu; tiplenmiş RegisterError karşılıkları) ---
    const val EMAIL_TAKEN = "Bu e-posta adresi zaten kayıtlı."
    const val PHONE_TAKEN = "Bu telefon numarası zaten kayıtlı."
    const val INVALID_REFERRAL = "Davet kodu geçersiz."
    const val REGISTER_NETWORK = "İnternet bağlantısı kurulamadı."
    const val REGISTER_UNKNOWN = "Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin."

    // --- Ödeme ---
    const val CARD_REQUIRED = "Lütfen bir kart seçin."

    /** İyzico sayfası kapandı ama tahsilat SUCCESS değil (iptal/red/yarım kalan 3DS). */
    const val IYZICO_NOT_COMPLETED =
        "Ödeme tamamlanmadı. Kartınızdan tutar çekilmedi; tekrar deneyebilirsiniz."

    // --- Selfie ---
    const val SELFIE_CAPTURE_FAILED = "Selfie çekilemedi. Lütfen tekrar deneyin."
}
