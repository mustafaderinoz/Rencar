package com.turkcell.rencar.ui.register

/** Telefon: ülke kodu (+90) hariç hane sayısı — Login ile aynı kural. */
const val PHONE_DIGIT_COUNT = 10

/** Şifre alt sınırı — openapi RegisterDto.password (minLength 6). */
const val PASSWORD_MIN_LENGTH = 6

/**
 * 02c Kayıt — saf UI durumu (§4.2).
 *
 * Login'de girilen numara kayıtlı değilse (POST /auth/login → 401) buraya gelinir; numara
 * [phone]'a önceden doldurulur ama düzenlenebilir kalır ("Kayıt ol" linkinden gelindiğinde boştur).
 * Davet kodu ([referralCode]) İSTEĞE BAĞLIdır; diğer alanlar zorunludur.
 *
 * Hata alanları ayrıdır çünkü sunucu e-posta ve telefon çakışmasının İKİSİNİ DE 409 ile döner;
 * hangi alana ait olduğu mapper katmanında ayrıştırılır (bkz. data/model/RegisterError).
 * [formError] alana bağlanamayan (ağ/doğrulama/bilinmeyen) hatalar içindir.
 */
data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val phone: String = "",
    val referralCode: String = "",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val phoneError: String? = null,
    val referralCodeError: String? = null,
    val formError: String? = null,
    val isLoading: Boolean = false,
    /** POST /auth/register başarılı → Login'e dönüş sinyali (§4.6: Effect yerine state bayrağı). */
    val registered: Boolean = false,
) {
    /** "Hesap Oluştur" etkinliği: zorunlu alanlar dolu (davet kodu hariç) ve istek uçmuyor. */
    val canSubmit: Boolean
        get() = fullName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            phone.length == PHONE_DIGIT_COUNT &&
            !isLoading
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli -> data class, parametresiz -> data object.
 */
sealed interface RegisterIntent {
    data class FullNameChanged(val fullName: String) : RegisterIntent
    data class EmailChanged(val email: String) : RegisterIntent
    data class PasswordChanged(val password: String) : RegisterIntent
    data class PhoneChanged(val phone: String) : RegisterIntent
    data class ReferralCodeChanged(val referralCode: String) : RegisterIntent
    data object SubmitClicked : RegisterIntent
    data object BackClicked : RegisterIntent
    data object LoginClicked : RegisterIntent

    /** Ekran [RegisterUiState.registered] geçişini yaptı → bayrak tüketilir. */
    data object RegisteredHandled : RegisterIntent
}
