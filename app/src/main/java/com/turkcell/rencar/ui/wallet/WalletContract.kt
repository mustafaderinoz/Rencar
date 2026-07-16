package com.turkcell.rencar.ui.wallet

import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.WalletUi

/** Kart Ekle pop-up'ındaki marka seçimi (ödeme ekranıyla aynı görsel meta). */
enum class WalletCardBrand { VISA, MASTERCARD }

/**
 * Cüzdan ekranı — saf UI durumu (§4.2). Açılışta GET /wallet (bakiye + son işlemler) + GET /cards
 * (kayıtlı kartlar) paralel yüklenir. Tüm alanlar varsayılan değerlidir; yalnızca saf UI durumunu
 * tutar. Navigasyon yoktur (Home sekmesi içi, kendi kendine yeten ekran; §4.5–4.6).
 */
data class WalletUiState(
    /** İlk yükleme (cüzdan + kartlar) sürüyor. */
    val isLoading: Boolean = true,
    /** İlk yükleme hatası (tam ekran hata + tekrar dene); yoksa null. Cüzdan kritiktir. */
    val loadError: String? = null,

    /** Cüzdan: bakiye + son işlemler; yüklenene kadar null. */
    val wallet: WalletUi? = null,
    /** Kayıtlı kartlar (öntanımlı en üstte). */
    val cards: List<CardUi> = emptyList(),

    /** Kart aksiyonu (varsayılan/sil) hatası — liste üstünde tek satır uyarı; yoksa null. */
    val cardActionError: String? = null,
    /** "Varsayılan yap" (PATCH /cards/{id}/default) sürerken o kartın id'si; yoksa null. */
    val settingDefaultCardId: String? = null,
    /** Silme onayı bekleyen kartın id'si (onay pop-up'ı açık); yoksa null. */
    val deleteCandidateCardId: String? = null,
    /** "Sil" (DELETE /cards/{id}) sürerken o kartın id'si; yoksa null. */
    val deletingCardId: String? = null,

    // ── Bakiye Yükle pop-up durumu ──
    /** Pop-up açık mı. */
    val showTopup: Boolean = false,
    /** Pop-up: tutar girişi (yalnız rakam). */
    val topupAmount: String = "",
    /** Pop-up: yükleme (POST /wallet/topup) sürüyor. */
    val isToppingUp: Boolean = false,
    /** Pop-up: yükleme hatası; yoksa null. */
    val topupError: String? = null,

    // ── Kart Ekle pop-up durumu ──
    /** Pop-up açık mı. */
    val showAddCard: Boolean = false,
    /** Pop-up: seçili marka. */
    val addCardBrand: WalletCardBrand = WalletCardBrand.VISA,
    /** Pop-up: son 4 hane girişi (yalnız rakam, maks 4). */
    val addCardLast4: String = "",
    /** Pop-up: SKT ay girişi (1-12). */
    val addCardMonth: String = "",
    /** Pop-up: SKT yıl girişi (4 hane). */
    val addCardYear: String = "",
    /** Pop-up: kaydetme (POST /cards) sürüyor. */
    val isAddingCard: Boolean = false,
    /** Pop-up: kaydetme hatası; yoksa null. */
    val addCardError: String? = null,
) {
    /** Cüzdan bakiyesi (₺); yüklenene kadar 0. */
    val balance: Double get() = wallet?.balance ?: 0.0

    /** "Bakiye Yükle" pop-up'ında "Yükle" aktif mi (tutar 10–5000 aralığında). */
    val canSubmitTopup: Boolean
        get() = !isToppingUp && (topupAmount.toIntOrNull() ?: 0) in 10..5000

    /** Kart Ekle pop-up'ında "Ekle" aktif mi (son 4 hane + geçerli ay/yıl). */
    val canSubmitCard: Boolean
        get() = !isAddingCard &&
            addCardLast4.length == 4 &&
            (addCardMonth.toIntOrNull() ?: 0) in 1..12 &&
            (addCardYear.toIntOrNull() ?: 0) in 2000..2100
}

/**
 * Kullanıcı aksiyonları (§4.3): parametreli → data class, parametresiz → data object.
 */
sealed interface WalletIntent {
    /** İlk yükleme hatası sonrası yeniden dener. */
    data object Retry : WalletIntent

    // ── Bakiye Yükle pop-up ──
    /** "+ Bakiye Yükle" → pop-up'ı açar. */
    data object TopupClicked : WalletIntent
    /** Pop-up "Vazgeç" / dışına dokunma → kapatır (alanı sıfırlar). */
    data object DismissTopup : WalletIntent
    /** Pop-up: tutar girişi. */
    data class TopupAmountChanged(val value: String) : WalletIntent
    /** Pop-up "Yükle" → POST /wallet/topup; başarıda cüzdan tazelenir. */
    data object SubmitTopup : WalletIntent

    // ── Kart aksiyonları ──
    /** "Varsayılan yap" → PATCH /cards/{id}/default; başarıda liste tazelenir. */
    data class SetDefaultCard(val cardId: String) : WalletIntent
    /** "Sil" → silme onayı pop-up'ını açar (doğrudan silmez). */
    data class DeleteCardClicked(val cardId: String) : WalletIntent
    /** Onay pop-up "Vazgeç" / dışına dokunma → onayı kapatır. */
    data object DismissDeleteCard : WalletIntent
    /** Onay pop-up "Sil" → DELETE /cards/{id}; başarıda liste tazelenir. */
    data object ConfirmDeleteCard : WalletIntent

    // ── Kart Ekle pop-up ──
    /** "+ Ekle" → pop-up'ı açar. */
    data object AddCardClicked : WalletIntent
    /** Pop-up "Vazgeç" / dışına dokunma → kapatır (alanları sıfırlar). */
    data object DismissAddCard : WalletIntent
    /** Pop-up: marka seçer. */
    data class AddCardBrandChanged(val brand: WalletCardBrand) : WalletIntent
    /** Pop-up: son 4 hane girişi. */
    data class AddCardLast4Changed(val value: String) : WalletIntent
    /** Pop-up: SKT ay girişi. */
    data class AddCardMonthChanged(val value: String) : WalletIntent
    /** Pop-up: SKT yıl girişi. */
    data class AddCardYearChanged(val value: String) : WalletIntent
    /** Pop-up "Ekle" → POST /cards; başarıda liste tazelenir. */
    data object SubmitAddCard : WalletIntent
}
