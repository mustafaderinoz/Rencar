package com.turkcell.rencar.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.turkcell.rencar.BuildConfig
import com.turkcell.rencar.data.model.VehicleUi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Önerisi Deposu: Gemini AI kullanarak kullanıcı sorgularına göre araç önerileri sunar.
 */
@Singleton
class AiRepository @Inject constructor() {

    private val model = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Kullanıcı sorgusuna göre araç listesini filtreler ve önerilen araçların ID'lerini döner.
     */
    suspend fun recommendVehicles(query: String, vehicles: List<VehicleUi>): Result<List<String>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()

        val vehiclesJson = vehicles.joinToString(separator = "\n") { v ->
            "ID: ${v.id}, Marka: ${v.brand}, Model: ${v.model}, Tip: ${v.type}, Günlük Fiyat: ${v.pricePerDay} TL, Koltuk: ${v.seats ?: 5}, Vites: ${v.transmission ?: "Bilinmiyor"}"
        }

        val prompt = """
            Aşağıdaki araç listesinden kullanıcı sorgusuna en uygun araçları seç.
            Yalnızca araçların ID'lerini içeren bir JSON listesi döndür. 
            Çıktı FORMATI KESİNLİKLE sadece saf bir JSON array olmalıdır, başka hiçbir metin ekleme.
            Örnek çıktı: ["id1", "id2"]
            Eğer uygun araç yoksa boş liste döndür: []
            
            Kullanıcı Sorgusu: $query
            
            Araç Listesi:
            $vehiclesJson
        """.trimIndent()

        val response = model.generateContent(content { text(prompt) })
        val responseText = response.text ?: "[]"
        
        // JSON dizisini ayrıştır (["id1", "id2"])
        json.parseToJsonElement(responseText).jsonArray.map { it.jsonPrimitive.content }
    }
}
