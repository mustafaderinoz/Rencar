package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.WalletTransactionUi
import com.turkcell.rencar.data.model.WalletUi
import com.turkcell.rencar.data.remote.dto.WalletResponse
import com.turkcell.rencar.data.remote.dto.WalletTransaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Cüzdan akışı DTO → UI modeli dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * İşaretli tutar etiketi ve göreli tarih ("Bugün · 14:32") burada üretilir; API şema değişiklikleri
 * yalnızca bu katmanda karşılanır.
 */

fun WalletResponse.toUi(): WalletUi = WalletUi(
    balance = balance,
    transactions = transactions.map { it.toUi() },
)

private fun WalletTransaction.toUi(): WalletTransactionUi {
    val credit = amount >= 0.0
    return WalletTransactionUi(
        id = id,
        title = description.ifBlank { defaultTitle(type) },
        dateLabel = relativeDateLabel(createdAt),
        amountLabel = "${if (credit) "+" else "−"}${formatTl(kotlin.math.abs(amount))}",
        isCredit = credit,
    )
}

/** Açıklama boşsa işlem tipinden Türkçe başlık üretir (öngörülmez, güvenli varsayılan). */
private fun defaultTitle(type: String): String = when (type.uppercase()) {
    "TOPUP" -> "Bakiye yükleme"
    "RENTAL_PAYMENT" -> "Yolculuk ödemesi"
    "REFERRAL_BONUS" -> "Davet bonusu"
    else -> "İşlem"
}

/** "₺340,00" / "₺1.250,50" — Türkçe biçim (virgül ondalık, nokta binlik). */
internal fun formatTl(value: Double): String = "₺%,.2f".format(Locale.forLanguageTag("tr"), value)

/**
 * ISO-8601 tarih-saat → göreli etiket: "Bugün · 14:32", "Dün · 09:10" veya "11.07.2026 · 12:00".
 * minSdk 24 + desugaring kapalı olduğundan java.time yerine [SimpleDateFormat]/[Calendar] kullanılır
 * ('X' ISO ofseti; RentalMapper ile aynı kalıp). Çözülemezse boş etiket döner.
 */
private fun relativeDateLabel(iso: String): String {
    val date = parseIso(iso) ?: return ""
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
    return when (daysAgo(date)) {
        0 -> "Bugün · $time"
        1 -> "Dün · $time"
        else -> "${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date)} · $time"
    }
}

private fun parseIso(iso: String): Date? {
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
    )
    for (pattern in patterns) {
        val parsed = runCatching { SimpleDateFormat(pattern, Locale.US).parse(iso) }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

/** [date] ile bugün arasındaki takvim-günü farkı (cihaz yerel saati): 0 = bugün, 1 = dün. */
private fun daysAgo(date: Date): Int {
    fun Calendar.startOfDay() = apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val then = Calendar.getInstance().apply { time = date }.startOfDay()
    val today = Calendar.getInstance().startOfDay()
    val diffMs = today.timeInMillis - then.timeInMillis
    return (diffMs / (24L * 60 * 60 * 1000)).toInt()
}
