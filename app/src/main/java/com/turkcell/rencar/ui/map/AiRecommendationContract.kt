package com.turkcell.rencar.ui.map

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
    val recommendedIds: List<String> = emptyList()
)

/**
 * AI Önerisi Intent'leri.
 */
sealed interface AiRecommendationIntent {
    /** Sorgu metni değişti. */
    data class QueryChanged(val query: String) : AiRecommendationIntent
    /** Sorgu gönderildi. */
    data object Submit : AiRecommendationIntent
    /** Öneriler temizlendi. */
    data object Clear : AiRecommendationIntent
}
