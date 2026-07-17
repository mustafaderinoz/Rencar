package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.RegisterError
import com.turkcell.rencar.data.model.UserUi
import com.turkcell.rencar.data.remote.dto.UserDto
import java.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.HttpException

/**
 * Kullanıcı DTO → UI modeli dönüşümü (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * API şema değişiklikleri yalnızca burada karşılanır ("Minimum Değişiklik İlkesi").
 */
fun UserDto.toUi(): UserUi = UserUi(
    id = id,
    email = email,
    phone = phone,
    fullName = fullName,
    role = role,
)

/**
 * POST /auth/register hatasını tiplenmiş [RegisterError]'a çevirir (ayrıştırma yalnız burada;
 * ViewModel sunucunun hata şemasını görmez).
 *
 * Sunucu davranışı (canlı v2 ile doğrulandı):
 * - 409 "Bu e-posta adresi zaten kayıtlı." / "Bu telefon numarası zaten kayıtlı."
 *   → aynı kod, YALNIZ metin ayırır; bu yüzden anahtar kelimeyle ayrıştırılır.
 * - 400 "Davet kodu geçersiz." (tek metin) veya alan doğrulama hataları (metin DİZİSİ).
 *
 * Doğrulama 400'ü de "e-posta"/"telefon" kelimelerini içerdiğinden ("Geçerli bir e-posta adresi
 * giriniz.") çakışma dalları 409'a bağlanmıştır — sıra ve kod koşulu bilinçlidir.
 * Eşlenemeyen 400/409'da sunucunun kendi metni [RegisterError.Validation] ile gösterilir; böylece
 * yeni bir iş kuralı hatası eklense bile kullanıcı anlamlı mesaj görür (metin uydurulmaz).
 */
fun Throwable.toRegisterError(): RegisterError = when (this) {
    is HttpException -> {
        val messages = errorMessages()
        when {
            code() == 409 && messages.anyContains("e-posta") -> RegisterError.EmailTaken
            code() == 409 && messages.anyContains("telefon") -> RegisterError.PhoneTaken
            code() == 400 && messages.anyContains("davet kodu") -> RegisterError.InvalidReferral
            messages.isNotEmpty() && code() in 400..499 -> RegisterError.Validation(messages)
            else -> RegisterError.Unknown
        }
    }

    is IOException -> RegisterError.Network
    else -> RegisterError.Unknown
}

/**
 * Hata gövdesindeki `message` alanını tek listeye indirger: doğrulama hatalarında dizi
 * (["Geçerli bir e-posta adresi giriniz.", …]), çakışma/iş kuralı hatalarında tek string gelir.
 * Gövde okunamaz, JSON değilse veya beklenen şekilde değilse boş liste döner (çağıran taraf
 * [RegisterError.Unknown]'a düşer).
 */
private fun HttpException.errorMessages(): List<String> = runCatching {
    val body = response()?.errorBody()?.string().orEmpty()
    when (val message = (ErrorJson.parseToJsonElement(body) as JsonObject)["message"]) {
        is JsonArray -> message.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> listOfNotNull(message.contentOrNull)
        else -> emptyList()
    }
}.getOrDefault(emptyList())

private fun List<String>.anyContains(keyword: String): Boolean =
    any { it.contains(keyword, ignoreCase = true) }

/** Hata gövdesi ayrıştırıcısı — mapper'a özel; DI'daki [Json] örneğine bağımlı kalmamak için ayrı. */
private val ErrorJson = Json { ignoreUnknownKeys = true }
