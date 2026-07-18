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

    suspend fun recommendVehicles(query: String, vehicles: List<VehicleUi>): Result<List<String>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()

        // segment bilgisi eksikti — eklendi, model artık ECONOMY/COMFORT/SUV ayrımını görebiliyor.
        val vehiclesJson = vehicles.joinToString(separator = "\n") { v ->
            "ID: ${v.id} | Marka-Model: ${v.brand} ${v.model} | Kasa Tipi: ${v.type} | " +
                    "Fiyat Segmenti: ${v.segment} | Günlük Fiyat: ${v.pricePerDay} TL | " +
                    "Menzil: ${v.rangeKm} Km | " +
                    "Koltuk: ${v.seats ?: 5} | Vites: ${v.transmission ?: "Bilinmiyor"}"
        }

        val prompt = """
            Sen bir araç kiralama asistanısın. Kullanıcının doğal dildeki isteğine göre,
            verilen listeden EN UYGUN araçların ID'lerini seç.

            Eşleştirme kuralları:
            - "lüks", "premium", "konforlu", "kaliteli" -> Fiyat Segmenti: COMFORT
            - "ucuz", "ekonomik", "uygun fiyatlı", "bütçe dostu", "en ucuz" -> Fiyat Segmenti: ECONOMY;
              sonuçları günlük fiyata göre ARTAN sırada listele (en ucuzu önce).
            - "arazi", "off-road", "4x4", "dağa/kırsala/köye gideceğim", "toprak yol" -> Kasa Tipi: SUV
            - "aile", "kalabalık", "çok kişi", "geniş" -> Koltuk sayısı yüksek araçlar veya
              Kasa Tipi: MINIVAN/STATION
            - "otomatik vites" -> Vites: AUTOMATIC, "manuel vites" -> Vites: MANUAL
            - Kullanıcı bir bütçe belirtirse (ör. "2000 TL altı"), günlük fiyatı bu sınırın
              ALTINDA olan araçları seç; bütçe + stil birlikte belirtilmişse (ör.
              "2000 TL altı lüks araç") HER İKİ kritere de uy: COMFORT segmentinde VE
              belirtilen fiyatın altında olanları seç.
            - Kullanıcı bir şehir/semt belirtse bile (ör. "Ankara'da") bunu KONUM FİLTRESİ
              olarak KULLANMA — elimizde araç konum verisi bu bağlamda yok; yalnızca fiyat,
              tip ve segment gibi diğer kriterlere odaklan.
            - Hiçbir açık kritere uyan araç yoksa, en yakın 3-5 aracı öner; TAMAMEN alakasız
              bir istekse (araç kiralamayla ilgisiz) boş liste döndür.

            Çıktı formatı KESİNLİKLE sadece bir JSON dizisi (araç ID'leri) olmalı, başka
            hiçbir açıklama veya metin ekleme.
            Örnek: ["id1", "id2", "id3"]

            Kullanıcı Sorgusu: "$query"

            Araç Listesi:
            $vehiclesJson
        """.trimIndent()

        val response = model.generateContent(content { text(prompt) })
        val responseText = response.text ?: "[]"

        json.parseToJsonElement(responseText).jsonArray.map { it.jsonPrimitive.content }
    }
}