package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Ödeme akışı DTO'ları — openapi.json şemalarıyla birebir (tag: Cards, Wallet, Rentals/pay).
 *
 * Kiralama bitince ([FinishRentalResponse]) ekran ödeme adımına geçer:
 * - GET /cards            → kayıtlı kartlar ([CardResponse]); öntanımlı en üstte.
 * - POST /cards           → yalnız marka + son 4 hane + SKT kaydeder ([CreateCardRequest]).
 * - GET /wallet           → cüzdan bakiyesi ([WalletResponse]); yalnız balance okunur.
 * - POST /rentals/{id}/pay → cüzdan/kartla öder ([PayRentalRequest] → [PayRentalResponse]).
 *
 * DTO'lar UI'a doğrudan verilmez; mapper katmanında modele çevrilir (decisions.md → "Katman Derinliği").
 */

/**
 * GET /cards öğesi + POST /cards / PATCH default cevabı (CardResponseDto). [brand] VISA | MASTERCARD;
 * [last4] kartın son 4 hanesi; [expMonth]/[expYear] SKT; [isDefault] ödemede öntanımlı gösterilen kart.
 * Tam kart numarası/CVV HİÇBİR zaman tutulmaz (backend PCI kapsamı dışı — yalnız görsel meta).
 */
@Serializable
data class CardResponse(
    val id: String,
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
    val isDefault: Boolean = false,
)

/**
 * POST /cards gövdesi (CreateCardDto). Yalnız marka + son 4 hane + SKT gönderilir; tam kart numarası
 * gönderilirse backend 400 döner (bkz. openapi.json). İlk kart otomatik öntanımlı olur.
 */
@Serializable
data class CreateCardRequest(
    val brand: String,
    val last4: String,
    val expMonth: Int,
    val expYear: Int,
)

/**
 * GET /wallet cevabı (WalletResponseDto). Ödeme ekranı yalnız [balance]'ı (cüzdan bakiyesi) okur;
 * transactions listesi bu akışta kullanılmaz (Json.ignoreUnknownKeys ile atlanır).
 */
@Serializable
data class WalletResponse(
    val balance: Double = 0.0,
)

/**
 * POST /rentals/{id}/pay gövdesi (PayRentalDto). [method] WALLET | CARD; CARD'da [cardId] zorunlu,
 * WALLET'ta verilmez. [discountCode] varsa indirim sunucuda uygulanır (opsiyonel). explicitNulls=false
 * olduğundan null alanlar gövdeye yazılmaz (WALLET'ta cardId=null → JSON'a hiç eklenmez).
 */
@Serializable
data class PayRentalRequest(
    val method: String,
    val cardId: String? = null,
    val discountCode: String? = null,
)

/** Ödemede kullanılan kart özeti (PaidCardSummaryDto) — yalnız CARD yönteminde dolu. */
@Serializable
data class PaidCardSummary(
    val brand: String,
    val last4: String,
)

/**
 * POST /rentals/{id}/pay 200 cevabı (PayRentalResponseDto) — ödeme makbuzu. [paidAmount] fiilen
 * ödenen tutar (indirim sonrası); [walletBalance] yalnız WALLET yönteminde dolu; [card] yalnız CARD'da.
 */
@Serializable
data class PayRentalResponse(
    val rentalId: String,
    val paymentStatus: String,
    val method: String,
    val totalPrice: Double = 0.0,
    val discountAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val walletBalance: Double? = null,
    val card: PaidCardSummary? = null,
)
