package com.turkcell.rencar.data.mapper

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Mapper katmanının ortak tarih yardımcıları. minSdk 24 + core library desugaring kapalı olduğundan
 * java.time yerine [SimpleDateFormat] kullanılır (Rental/Wallet mapper'larında ortak — tekrar önlenir).
 */

/** ISO-8601 → [Date]; 'X' deseni ofseti ("Z" veya "+03:00") çözer. Çözülemezse null. */
internal fun parseIso(iso: String): Date? {
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
