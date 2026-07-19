package com.turkcell.rencar.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.turkcell.rencar.data.mapper.parseRecommendedIds
import com.turkcell.rencar.data.mapper.toPromptLine
import com.turkcell.rencar.data.model.VehicleUi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Doğal dil sorgusuna göre araç önerisi (Gemini). Kütüphane DI ardındadır: [model] enjekte edilir
 * (bkz. [com.turkcell.rencar.di.AiModule]); araç betimi ve yanıt ayrıştırma mapper katmanındadır
 * (bkz. `data/mapper/AiMapper`). Repository yalnızca eşleştirme kurallarını (prompt) ve model
 * çağrısını orkestre eder — RentalRepository'nin tarih/iş kuralını kapsüllemesiyle aynı desen.
 */
@Singleton
class AiRepository @Inject constructor(
    private val model: GenerativeModel,
) {

    suspend fun recommendVehicles(query: String, vehicles: List<VehicleUi>): Result<List<String>> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()

        val vehiclesJson = vehicles.joinToString(separator = "\n") { it.toPromptLine() }

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
        parseRecommendedIds(response.text ?: "[]")
    }
}
