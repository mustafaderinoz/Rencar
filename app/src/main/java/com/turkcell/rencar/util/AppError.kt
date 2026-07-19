package com.turkcell.rencar.util

import java.io.IOException
import retrofit2.HttpException

/**
 * Uygulama genelinde TEK TİP hata modeli.
 *
 * Retrofit/OkHttp katmanından yükselen ham [Throwable]'lar [toAppError] ile buraya indirgenir;
 * kullanıcıya gösterilecek metin ise yalnızca [ErrorMessages] (`AppError.toUserMessage`) tarafından
 * üretilir. ViewModel'ler hiçbir hata metni tutmaz.
 *
 * Kütüphane sınırı: `HttpException`/`IOException` bilgisi bu dosyada emilir; ui katmanı yalnız
 * [AppError] görür (decisions.md → "Minimum Değişiklik İlkesi").
 */
sealed class AppError : Throwable() {

    /** Ağa hiç ulaşılamadı (bağlantı yok, zaman aşımı, DNS…). */
    data object Network : AppError()

    /** Sunucu yanıt verdi ama hata kodu döndü. HTTP kodu KORUNUR (bağlama göre çözümlenir). */
    data class Api(val code: Int) : AppError()

    /** Sınıflandırılamayan hata; ham hata teşhis için taşınır (kullanıcıya gösterilmez). */
    data class Unknown(val original: Throwable? = null) : AppError()
}

/**
 * Ham hatayı [AppError]'a normalize eder. Zaten [AppError] olan hatalar (ör. repository'den
 * tiplenmiş gelenler) olduğu gibi geçirilir.
 */
fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is HttpException -> AppError.Api(code())
    is IOException -> AppError.Network
    else -> AppError.Unknown(this)
}

/**
 * 401 — token yok/geçersiz. Bazı ekranlarda bu bir HATA MESAJI değil AKIŞ SİNYALİdir (Login'de
 * "kayıtlı kullanıcı yok" → kayıt ekranı, Splash'te "oturum ölmüş" → login), bu yüzden metne
 * çevrilmeden önce ayrıca sorgulanabilir.
 */
val AppError.isUnauthorized: Boolean
    get() = this is AppError.Api && code == 401
