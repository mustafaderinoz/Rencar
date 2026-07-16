package com.turkcell.rencar.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.PaymentRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Ödeme ekranının tek durum kaynağı (§4.4). Açılışta üç çağrı paralel yüklenir: GET /rentals/{id}
 * (ücret dökümü), GET /cards (kayıtlı kartlar), GET /wallet (bakiye). Kullanıcı yöntem seçer
 * (Cüzdan/Kart), opsiyonel indirim kodu girer ve öder (POST /rentals/{id}/pay). Kart Ekle pop-up'ı
 * POST /cards ile yeni kart kaydeder; liste tazelenip yeni kart seçilir.
 *
 * Ayrı domain/UseCase katmanı yoktur (decisions.md); iş mantığı repository ardındadır.
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val rentalId: String =
        savedStateHandle.get<String>(RencarDestinations.PAYMENT_ARG_RENTAL_ID).orEmpty()

    private val _uiState = MutableStateFlow(PaymentUiState(rentalId = rentalId))
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: PaymentIntent) {
        when (intent) {
            PaymentIntent.Retry -> load()
            is PaymentIntent.SelectMethod -> _uiState.update { it.copy(method = intent.method, payError = null) }
            is PaymentIntent.SelectCard -> _uiState.update { it.copy(selectedCardId = intent.cardId) }
            is PaymentIntent.SetDefaultCard -> setDefaultCard(intent.cardId)
            is PaymentIntent.DeleteCardClicked ->
                _uiState.update { it.copy(deleteCandidateCardId = intent.cardId) }
            PaymentIntent.DismissDeleteCard ->
                _uiState.update { if (it.deletingCardId != null) it else it.copy(deleteCandidateCardId = null) }
            PaymentIntent.ConfirmDeleteCard -> _uiState.value.deleteCandidateCardId?.let { deleteCard(it) }
            is PaymentIntent.DiscountCodeChanged -> _uiState.update { it.copy(discountCode = intent.code) }
            PaymentIntent.PayClicked -> pay()

            PaymentIntent.AddCardClicked -> _uiState.update { it.copy(showAddCard = true, addCardError = null) }
            PaymentIntent.DismissAddCard -> dismissAddCard()
            is PaymentIntent.AddCardBrandChanged -> _uiState.update { it.copy(addCardBrand = intent.brand) }
            is PaymentIntent.AddCardLast4Changed ->
                _uiState.update { it.copy(addCardLast4 = intent.value.filter { c -> c.isDigit() }.take(4)) }
            is PaymentIntent.AddCardMonthChanged ->
                _uiState.update { it.copy(addCardMonth = intent.value.filter { c -> c.isDigit() }.take(2)) }
            is PaymentIntent.AddCardYearChanged ->
                _uiState.update { it.copy(addCardYear = intent.value.filter { c -> c.isDigit() }.take(4)) }
            PaymentIntent.SubmitAddCard -> submitAddCard()
        }
    }

    /** Döküm + kart + bakiyeyi paralel yükler. Döküm başarısızsa tam ekran hata; kart/bakiye
     * hataları sessizce boş/0 sayılır (ödeme dökümü olmadan anlamsızdır, o yüzden döküm kritiktir). */
    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val receiptDeferred = async { paymentRepository.getReceipt(rentalId) }
            val cardsDeferred = async { paymentRepository.getCards() }
            val walletDeferred = async { paymentRepository.getWalletBalance() }

            receiptDeferred.await()
                .onSuccess { receipt ->
                    val cards = cardsDeferred.await().getOrDefault(emptyList())
                    val balance = walletDeferred.await().getOrDefault(0.0)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = null,
                            receipt = receipt,
                            cards = cards,
                            walletBalance = balance,
                            selectedCardId = cards.firstOrNull { c -> c.isDefault }?.id
                                ?: cards.firstOrNull()?.id,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.toLoadMessage()) }
                }
        }
    }

    private fun pay() {
        val state = _uiState.value
        val receipt = state.receipt ?: return
        if (!state.canPay || receipt.alreadyPaid) return

        _uiState.update { it.copy(isPaying = true, payError = null) }
        val code = state.discountCode.trim().ifBlank { null }
        viewModelScope.launch {
            val result = when (state.method) {
                PaymentMethod.WALLET -> paymentRepository.payWithWallet(rentalId, code)
                PaymentMethod.CARD -> {
                    val cardId = state.selectedCardId
                    if (cardId == null) {
                        _uiState.update { it.copy(isPaying = false, payError = "Lütfen bir kart seçin.") }
                        return@launch
                    }
                    paymentRepository.payWithCard(rentalId, cardId, code)
                }
            }
            result
                .onSuccess { res -> _uiState.update { it.copy(isPaying = false, result = res) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isPaying = false, payError = e.toPayMessage(hasDiscount = code != null)) }
                }
        }
    }

    /** PATCH /cards/{id}/default: kartı öntanımlı yapar; başarıda tazelenmiş liste + kart seçili. */
    private fun setDefaultCard(cardId: String) {
        val state = _uiState.value
        if (state.settingDefaultCardId != null) return
        // Zaten öntanımlıysa bir şey yapma (gereksiz ağ çağrısı).
        if (state.cards.firstOrNull { it.id == cardId }?.isDefault == true) return

        _uiState.update { it.copy(settingDefaultCardId = cardId) }
        viewModelScope.launch {
            paymentRepository.setDefaultCard(cardId)
                .onSuccess { cards ->
                    _uiState.update {
                        it.copy(settingDefaultCardId = null, cards = cards, selectedCardId = cardId)
                    }
                }
                .onFailure { _uiState.update { it.copy(settingDefaultCardId = null) } }
        }
    }

    /** DELETE /cards/{id}: kartı siler (onaydan sonra); başarıda liste tazelenir, seçim geçerli karta düşer. */
    private fun deleteCard(cardId: String) {
        if (_uiState.value.deletingCardId != null) return

        // Onay pop-up'ı kapanır, silme sürerken o kartta spinner gösterilir.
        _uiState.update { it.copy(deletingCardId = cardId, deleteCandidateCardId = null) }
        viewModelScope.launch {
            paymentRepository.deleteCard(cardId)
                .onSuccess { cards ->
                    _uiState.update {
                        // Silinen kart seçiliyse seçimi öntanımlıya (yoksa ilk karta, o da yoksa null) taşı.
                        val selected = if (it.selectedCardId == cardId) {
                            cards.firstOrNull { c -> c.isDefault }?.id ?: cards.firstOrNull()?.id
                        } else {
                            it.selectedCardId
                        }
                        it.copy(deletingCardId = null, cards = cards, selectedCardId = selected)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(deletingCardId = null, payError = e.toDeleteCardMessage()) }
                }
        }
    }

    private fun submitAddCard() {
        val state = _uiState.value
        if (!state.canSubmitCard) return
        val month = state.addCardMonth.toIntOrNull() ?: return
        val year = state.addCardYear.toIntOrNull() ?: return

        _uiState.update { it.copy(isAddingCard = true, addCardError = null) }
        viewModelScope.launch {
            paymentRepository.addCard(
                brand = state.addCardBrand.name,
                last4 = state.addCardLast4,
                expMonth = month,
                expYear = year,
            )
                .onSuccess { added ->
                    // Kaydedilen kartı sunucudan tazelenmiş listeyle göster (öntanımlı sırası doğru gelsin).
                    val refreshed = paymentRepository.getCards().getOrDefault(listOf(added))
                    _uiState.update {
                        it.copy(
                            isAddingCard = false,
                            showAddCard = false,
                            cards = refreshed,
                            method = PaymentMethod.CARD,
                            selectedCardId = added.id,
                            addCardBrand = CardBrand.VISA,
                            addCardLast4 = "",
                            addCardMonth = "",
                            addCardYear = "",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isAddingCard = false, addCardError = e.toAddCardMessage()) }
                }
        }
    }

    private fun dismissAddCard() {
        _uiState.update {
            it.copy(
                showAddCard = false,
                addCardBrand = CardBrand.VISA,
                addCardLast4 = "",
                addCardMonth = "",
                addCardYear = "",
                addCardError = null,
            )
        }
    }

    private fun Throwable.toLoadMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            404 -> "Ödenecek yolculuk bulunamadı."
            else -> "Ödeme bilgileri alınamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    /**
     * [hasDiscount] true ise (indirim kodu gönderilmişti) 404/409 mesajları kod-özel hale getirilir:
     * API'de indirim kodu doğrulama ucu olmadığından geçersiz kod ancak ödeme anında anlaşılır.
     */
    private fun Throwable.toPayMessage(hasDiscount: Boolean): String = when (this) {
        is HttpException -> when (code()) {
            400 -> "Ödeme bilgileri geçersiz."
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            403 -> "Bu yolculuk size ait değil."
            404 -> if (hasDiscount) {
                "Girdiğiniz indirim kodu bulunamadı. Kodu kontrol edin ya da kaldırıp tekrar deneyin."
            } else {
                "Yolculuk veya kart bulunamadı."
            }
            409 -> if (hasDiscount) {
                "İndirim kodu kullanılamıyor (limit dolmuş veya daha önce kullanılmış) ya da bakiye yetersiz. " +
                    "Kodu kaldırıp tekrar deneyebilirsiniz."
            } else {
                "Ödeme alınamadı: cüzdan bakiyesi yetersiz olabilir veya yolculuk zaten ödenmiş."
            }
            else -> "Ödeme alınamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toDeleteCardMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            404 -> "Kart bulunamadı."
            else -> "Kart silinemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toAddCardMessage(): String = when (this) {
        is HttpException -> when (code()) {
            400 -> "Kart bilgileri geçersiz (son kullanma tarihi geçmiş olabilir)."
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            else -> "Kart kaydedilemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
