package com.turkcell.rencar.ui.map

import com.turkcell.rencar.data.model.VehicleUi

/**
 * AI Önerisi UI Durumu.
 */
data class AiRecommendationUiState(
    /** Kullanıcının girdiği sorgu metni. */
    val query: String = "",
    /** AI yanıtı bekleniyor mu. */
    val isLoading: Boolean = false,
    /** Hata mesajı (varsa). */
    val error: String? = null,
    /** AI tarafından önerilen araç ID'leri. */
    val recommendedIds: List<String> = emptyList(),
    /** Harita ekranından sağlanan aday araçlar; öneri bu listeden seçilir (tek doğruluk kaynağı). */
    val vehicles: List<VehicleUi> = emptyList(),
)

/**
 * AI Önerisi Intent'leri.
 */
sealed interface AiRecommendationIntent {
    /** Sorgu metni değişti. */
    data class QueryChanged(val query: String) : AiRecommendationIntent
    /** Harita ekranı aday araç listesini sağladı (diyalog açılışında). */
    data class VehiclesProvided(val vehicles: List<VehicleUi>) : AiRecommendationIntent
    /** Sorgu gönderildi (öneri state'teki araçlardan seçilir). */
    data object Submit : AiRecommendationIntent
    /** Öneriler temizlendi. */
    data object Clear : AiRecommendationIntent
    /** Diyalog kapatıldı — kapatma Screen katmanında ele alınır (§4.5/§4.6). */
    data object Dismiss : AiRecommendationIntent
}
