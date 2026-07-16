package com.turkcell.rencar.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.PaymentRepository
import com.turkcell.rencar.data.repository.WalletRepository
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
 * Cüzdan ekranının tek durum kaynağı (§4.4). Açılışta iki çağrı paralel yüklenir: GET /wallet
 * (bakiye + son işlemler, kritik) ve GET /cards (kayıtlı kartlar; hatası sessizce boş sayılır).
 * Kullanıcı bakiye yükler (POST /wallet/topup), kart ekler/siler/öntanımlı yapar.
 *
 * Kart yönetimi mevcut [PaymentRepository] üzerinden yeniden kullanılır (decisions.md → tek kaynak,
 * kod tekrarı yok); cüzdan işlemleri [WalletRepository] ardındadır. Ayrı UseCase katmanı yoktur.
 */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val paymentRepository: PaymentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WalletUiState())
    val uiState: StateFlow<WalletUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onIntent(intent: WalletIntent) {
        when (intent) {
            WalletIntent.Retry -> load()

            WalletIntent.TopupClicked -> _uiState.update { it.copy(showTopup = true, topupError = null) }
            WalletIntent.DismissTopup -> dismissTopup()
            is WalletIntent.TopupAmountChanged ->
                _uiState.update { it.copy(topupAmount = intent.value.filter { c -> c.isDigit() }.take(4)) }
            WalletIntent.SubmitTopup -> submitTopup()

            is WalletIntent.SetDefaultCard -> setDefaultCard(intent.cardId)
            is WalletIntent.DeleteCardClicked ->
                _uiState.update { it.copy(deleteCandidateCardId = intent.cardId) }
            WalletIntent.DismissDeleteCard ->
                _uiState.update { if (it.deletingCardId != null) it else it.copy(deleteCandidateCardId = null) }
            WalletIntent.ConfirmDeleteCard -> _uiState.value.deleteCandidateCardId?.let { deleteCard(it) }

            WalletIntent.AddCardClicked -> _uiState.update { it.copy(showAddCard = true, addCardError = null) }
            WalletIntent.DismissAddCard -> dismissAddCard()
            is WalletIntent.AddCardBrandChanged -> _uiState.update { it.copy(addCardBrand = intent.brand) }
            is WalletIntent.AddCardLast4Changed ->
                _uiState.update { it.copy(addCardLast4 = intent.value.filter { c -> c.isDigit() }.take(4)) }
            is WalletIntent.AddCardMonthChanged ->
                _uiState.update { it.copy(addCardMonth = intent.value.filter { c -> c.isDigit() }.take(2)) }
            is WalletIntent.AddCardYearChanged ->
                _uiState.update { it.copy(addCardYear = intent.value.filter { c -> c.isDigit() }.take(4)) }
            WalletIntent.SubmitAddCard -> submitAddCard()
        }
    }

    /** Cüzdan + kartları paralel yükler. Cüzdan başarısızsa tam ekran hata; kart hatası sessizce boş. */
    private fun load() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch {
            val walletDeferred = async { walletRepository.getWallet() }
            val cardsDeferred = async { paymentRepository.getCards() }

            walletDeferred.await()
                .onSuccess { wallet ->
                    val cards = cardsDeferred.await().getOrDefault(emptyList())
                    _uiState.update {
                        it.copy(isLoading = false, loadError = null, wallet = wallet, cards = cards)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, loadError = e.toLoadMessage()) }
                }
        }
    }

    private fun submitTopup() {
        val state = _uiState.value
        if (!state.canSubmitTopup) return
        val amount = state.topupAmount.toIntOrNull()?.toDouble() ?: return

        _uiState.update { it.copy(isToppingUp = true, topupError = null) }
        viewModelScope.launch {
            walletRepository.topup(amount)
                .onSuccess { wallet ->
                    _uiState.update {
                        it.copy(isToppingUp = false, showTopup = false, topupAmount = "", wallet = wallet)
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isToppingUp = false, topupError = e.toTopupMessage()) }
                }
        }
    }

    /** PATCH /cards/{id}/default: kartı öntanımlı yapar; başarıda tazelenmiş liste. */
    private fun setDefaultCard(cardId: String) {
        val state = _uiState.value
        if (state.settingDefaultCardId != null) return
        // Zaten öntanımlıysa bir şey yapma (gereksiz ağ çağrısı).
        if (state.cards.firstOrNull { it.id == cardId }?.isDefault == true) return

        _uiState.update { it.copy(settingDefaultCardId = cardId, cardActionError = null) }
        viewModelScope.launch {
            paymentRepository.setDefaultCard(cardId)
                .onSuccess { cards -> _uiState.update { it.copy(settingDefaultCardId = null, cards = cards) } }
                .onFailure { e ->
                    _uiState.update { it.copy(settingDefaultCardId = null, cardActionError = e.toCardActionMessage()) }
                }
        }
    }

    /** DELETE /cards/{id}: kartı siler (onaydan sonra); başarıda liste tazelenir. */
    private fun deleteCard(cardId: String) {
        if (_uiState.value.deletingCardId != null) return

        // Onay pop-up'ı kapanır, silme sürerken o kartta spinner gösterilir.
        _uiState.update { it.copy(deletingCardId = cardId, deleteCandidateCardId = null, cardActionError = null) }
        viewModelScope.launch {
            paymentRepository.deleteCard(cardId)
                .onSuccess { cards -> _uiState.update { it.copy(deletingCardId = null, cards = cards) } }
                .onFailure { e ->
                    _uiState.update { it.copy(deletingCardId = null, cardActionError = e.toCardActionMessage()) }
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
                            addCardBrand = WalletCardBrand.VISA,
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

    private fun dismissTopup() {
        _uiState.update {
            if (it.isToppingUp) it else it.copy(showTopup = false, topupAmount = "", topupError = null)
        }
    }

    private fun dismissAddCard() {
        _uiState.update {
            if (it.isAddingCard) return@update it
            it.copy(
                showAddCard = false,
                addCardBrand = WalletCardBrand.VISA,
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
            403 -> "Cüzdana erişim için hesabınızın onaylı olması gerekir."
            else -> "Cüzdan bilgileri alınamadı (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toTopupMessage(): String = when (this) {
        is HttpException -> when (code()) {
            400 -> "Tutar 10 – 5.000 ₺ aralığında olmalı."
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            else -> "Bakiye yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }

    private fun Throwable.toCardActionMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            404 -> "Kart bulunamadı."
            else -> "İşlem tamamlanamadı (${code()}). Lütfen tekrar deneyin."
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
