package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.VehicleUi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * AI önerisi ⇄ model dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 *
 * Gemini isteğine giden araç betimi ve yanıtın ayrıştırılması repository gövdesinde DEĞİL, burada
 * yapılır: repository yalnızca orkestrasyonu (prompt kuralları + model çağrısı) yürütür.
 */

private val aiJson = Json { ignoreUnknownKeys = true }

/**
 * Modele gönderilecek tek satırlık araç betimi. Segment/koltuk/vites dâhildir; model
 * ECONOMY/COMFORT/SUV ve otomatik/manuel ayrımını görebilsin. Eksik alanlar güvenli
 * varsayılana (koltuk 5, vites "Bilinmiyor") iner.
 */
fun VehicleUi.toPromptLine(): String =
    "ID: $id | Marka-Model: $brand $model | Kasa Tipi: $type | " +
        "Fiyat Segmenti: $segment | Günlük Fiyat: $pricePerDay TL | " +
        "Menzil: $rangeKm Km | Koltuk: ${seats ?: 5} | Vites: ${transmission ?: "Bilinmiyor"}"

/**
 * Gemini'nin JSON dizi yanıtını (`["id1","id2"]`) araç ID listesine çevirir. Bozuk/eksik yanıtta
 * fırlatılan hata, çağıran repository'nin `runCatching`'i tarafından yakalanır (Result.failure).
 */
fun parseRecommendedIds(responseText: String): List<String> =
    aiJson.parseToJsonElement(responseText).jsonArray.map { it.jsonPrimitive.content }
