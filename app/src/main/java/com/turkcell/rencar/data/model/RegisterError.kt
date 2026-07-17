package com.turkcell.rencar.data.model

/**
 * Kayıt (POST /auth/register) hatası — UI/domain modeli (decisions.md → "Katman Derinliği").
 *
 * API'nin hata gövdesi UI'a sızmaz: HTTP kodu, Türkçe `message` metni ve `message`'ın değişken
 * şekli (409'da tek String, doğrulama 400'ünde List<String>) ayrı mapper katmanında
 * ([com.turkcell.rencar.data.mapper.toRegisterError]) bu tiplere eşlenir; ViewModel yalnız bu
 * tipleri görür. Sunucu metni/şekli değişirse yalnız mapper güncellenir ("Minimum Değişiklik").
 *
 * NOT: [EmailTaken] ve [PhoneTaken] AYNI HTTP kodunu (409) döner; yalnızca gövde metni ayırır.
 * Bu yüzden alan-bazlı hata göstermek gövdeyi okumayı gerektirir.
 */
sealed interface RegisterError {

    /** 409 — "Bu e-posta adresi zaten kayıtlı." */
    data object EmailTaken : RegisterError

    /** 409 — "Bu telefon numarası zaten kayıtlı." */
    data object PhoneTaken : RegisterError

    /** 400 — "Davet kodu geçersiz." */
    data object InvalidReferral : RegisterError

    /** 400 — sunucu alan doğrulama hataları; [messages] kullanıcıya gösterilebilir Türkçe metinler. */
    data class Validation(val messages: List<String>) : RegisterError

    /** Ağ/bağlantı hatası (IOException). */
    data object Network : RegisterError

    /** Beklenmeyen ya da eşlenemeyen hata. */
    data object Unknown : RegisterError
}

/**
 * [com.turkcell.rencar.data.repository.AuthRepository.register] başarısızlığını tiplenmiş [error]
 * ile taşır; böylece ViewModel `Result.failure` gövdesinden sunucu şemasına hiç dokunmadan
 * alan-bazlı mesaj üretebilir.
 */
class RegisterException(val error: RegisterError) : Exception()
