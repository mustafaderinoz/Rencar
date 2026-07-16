package com.turkcell.rencar.ui.payment

import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.PaymentReceiptUi
import com.turkcell.rencar.data.model.PaymentResultUi

/** Ödeme yöntemi sekmesi (Cüzdan / Kart) — tasarımdaki iki seçenekli geçiş. */
enum class PaymentMethod { WALLET, CARD }

/** Kart Ekle pop-up'ındaki marka seçimi. */
enum class CardBrand { VISA, MASTERCARD }

/**
 * Ödeme ekranı — saf UI durumu (§4.2). Kiralama bitince ([ActiveRental] finish) açılır; açılışta
 * GET /rentals/{id} (döküm) + GET /cards (kartlar) + GET /wallet (bakiye) yüklenir. Tüm alanlar
 * varsayılan değerlidir. Yalnızca saf UI durumunu tutar; navigasyon ekran katmanındadır (§4.5–4.6).
 */
data class PaymentUiState(
    /** İlk yükleme (döküm + kart + bakiye) sürüyor. */
    val isLoading: Boolean = true,
    /** İlk yükleme hatası (tam ekran hata + tekrar dene); yoksa null. */
    val loadError: String? = null,

    /** Kiralama kimliği (nav argümanı; pay için kullanılır). */
    val rentalId: String = "",
    /** Ücret dökümü + araç/plan bilgisi; yüklenene kadar null. */
    val receipt: PaymentReceiptUi? = null,

    /** Seçili ödeme yöntemi (varsayılan Cüzdan). */
    val method: PaymentMethod = PaymentMethod.WALLET,
    /** Cüzdan bakiyesi (₺). */
    val walletBalance: Double = 0.0,

    /** Kayıtlı kartlar (öntanımlı en üstte). */
    val cards: List<CardUi> = emptyList(),
    /** Seçili kartın id'si (Kart yönteminde); yoksa null. */
    val selectedCardId: String? = null,
    /** "Varsayılan yap" (PATCH /cards/{id}/default) sürerken o kartın id'si; yoksa null. */
    val settingDefaultCardId: String? = null,
    /** Silme onayı bekleyen kartın id'si (onay pop-up'ı açık); yoksa null. */
    val deleteCandidateCardId: String? = null,
    /** "Sil" (DELETE /cards/{id}) sürerken o kartın id'si; yoksa null. */
    val deletingCardId: String? = null,

    /** İndirim kodu alanı (opsiyonel). */
    val discountCode: String = "",

    /** POST /rentals/{id}/pay sürüyor (buton spinner). */
    val isPaying: Boolean = false,
    /** Ödeme hatası (buton üstünde); yoksa null. */
    val payError: String? = null,
    /** Ödeme başarılı olduğunda dolan makbuz; dolunca ekran başarı durumuna geçer. */
    val result: PaymentResultUi? = null,

    // ── Kart Ekle pop-up durumu ──
    /** Pop-up açık mı. */
    val showAddCard: Boolean = false,
    /** Pop-up: seçili marka. */
    val addCardBrand: CardBrand = CardBrand.VISA,
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
    /** Ödeme tamamlandı mı (pay başarılı). */
    val isPaid: Boolean get() = result != null

    /** "X ₺ Öde" butonundaki tutar (dökümdeki toplam; döküm yoksa 0). */
    val payableAmount: Double get() = receipt?.totalPrice ?: 0.0

    /** Cüzdan yetersiz mi (Cüzdan seçiliyken bakiye < toplam). Öde butonu buna göre kapatılır. */
    val walletInsufficient: Boolean
        get() = method == PaymentMethod.WALLET && walletBalance < payableAmount

    /** Kart yöntemiyle ödeme mümkün mü (seçili kart var). */
    private val cardReady: Boolean get() = method != PaymentMethod.CARD || selectedCardId != null

    /** Öde butonu aktif mi. */
    val canPay: Boolean
        get() = receipt != null && !receipt.alreadyPaid && !isPaying && !walletInsufficient && cardReady

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
sealed interface PaymentIntent {
    /** İlk yükleme hatası sonrası yeniden dener. */
    data object Retry : PaymentIntent

    /** Ödeme yöntemi sekmesini değiştirir (Cüzdan / Kart). */
    data class SelectMethod(val method: PaymentMethod) : PaymentIntent

    /** Kart listesinde bir kartı seçer (bu ödeme için). */
    data class SelectCard(val cardId: String) : PaymentIntent

    /** "Varsayılan yap" → PATCH /cards/{id}/default; başarıda liste tazelenir, kart seçili olur. */
    data class SetDefaultCard(val cardId: String) : PaymentIntent

    /** Çöp kutusu → silme onayı pop-up'ını açar (doğrudan silmez). */
    data class DeleteCardClicked(val cardId: String) : PaymentIntent
    /** Onay pop-up "Vazgeç" / dışına dokunma → onayı kapatır. */
    data object DismissDeleteCard : PaymentIntent
    /** Onay pop-up "Sil" → DELETE /cards/{id}; başarıda liste tazelenir, seçim geçerli karta düşürülür. */
    data object ConfirmDeleteCard : PaymentIntent

    /** İndirim kodu alanını günceller. */
    data class DiscountCodeChanged(val code: String) : PaymentIntent

    /** "X ₺ Öde" → POST /rentals/{id}/pay (seçili yöntemle). */
    data object PayClicked : PaymentIntent

    // ── Kart Ekle pop-up ──
    /** "+ Yeni kart ekle" → pop-up'ı açar. */
    data object AddCardClicked : PaymentIntent
    /** Pop-up "Vazgeç" / dışına dokunma → kapatır (alanları sıfırlar). */
    data object DismissAddCard : PaymentIntent
    /** Pop-up: marka seçer. */
    data class AddCardBrandChanged(val brand: CardBrand) : PaymentIntent
    /** Pop-up: son 4 hane girişi. */
    data class AddCardLast4Changed(val value: String) : PaymentIntent
    /** Pop-up: SKT ay girişi. */
    data class AddCardMonthChanged(val value: String) : PaymentIntent
    /** Pop-up: SKT yıl girişi. */
    data class AddCardYearChanged(val value: String) : PaymentIntent
    /** Pop-up "Ekle" → POST /cards; başarıda liste tazelenir, yeni kart seçilir. */
    data object SubmitAddCard : PaymentIntent
}
