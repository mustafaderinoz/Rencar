package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toBalance
import com.turkcell.rencar.data.mapper.toPaymentReceipt
import com.turkcell.rencar.data.mapper.toResult
import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.PaymentReceiptUi
import com.turkcell.rencar.data.model.PaymentResultUi
import com.turkcell.rencar.data.remote.api.CardApi
import com.turkcell.rencar.data.remote.api.RentalApi
import com.turkcell.rencar.data.remote.api.WalletApi
import com.turkcell.rencar.data.remote.dto.CreateCardRequest
import com.turkcell.rencar.data.remote.dto.PayRentalRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Ödeme iş akışı (karar: decisions.md → data + repository, ayrı mapper katmanı). ViewModel →
 * PaymentRepository → CardApi/WalletApi/RentalApi. Cüzdan/kart ödeme domain'i RentalRepository'den
 * ayrı tutulur (tek sorumluluk). Hata yönetimi Result ile çağırana taşınır; mesaj eşlemesi ViewModel'de.
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val cardApi: CardApi,
    private val walletApi: WalletApi,
    private val rentalApi: RentalApi,
) {
    /** GET /rentals/{id}: bitmiş kiralamanın ücret dökümü (ödeme ekranı üst özeti). */
    suspend fun getReceipt(rentalId: String): Result<PaymentReceiptUi> =
        runCatching { rentalApi.getRental(rentalId).toPaymentReceipt() }

    /** GET /cards: kayıtlı kartlar (öntanımlı en üstte). */
    suspend fun getCards(): Result<List<CardUi>> =
        runCatching { cardApi.list().map { it.toUi() } }

    /** GET /wallet: cüzdan bakiyesi (cüzdanla ödeme seçeneğinde gösterilir). */
    suspend fun getWalletBalance(): Result<Double> =
        runCatching { walletApi.getWallet().toBalance() }

    /**
     * POST /cards: kartı kaydeder (yalnız marka + son 4 hane + SKT). [brand] "VISA"/"MASTERCARD".
     * Kaydedilen kartı (öntanımlıysa isDefault=true) döner; çağıran listeyi tazeler.
     */
    suspend fun addCard(brand: String, last4: String, expMonth: Int, expYear: Int): Result<CardUi> =
        runCatching { cardApi.create(CreateCardRequest(brand, last4, expMonth, expYear)).toUi() }

    /**
     * PATCH /cards/{id}/default: seçilen kartı öntanımlı yapar; önceki öntanımlının işareti kalkar.
     * Başarıda güncel (öntanımlısı doğru sıralı) kart listesini döner; çağıran doğrudan state'e yazar.
     */
    suspend fun setDefaultCard(cardId: String): Result<List<CardUi>> = runCatching {
        cardApi.setDefault(cardId)
        cardApi.list().map { it.toUi() }
    }

    /**
     * DELETE /cards/{id}: kartı siler. Silinen kart öntanımlıysa backend kalan en yeni kartı
     * öntanımlı yapar. Başarıda güncel listeyi döner; 4xx dönerse Result.failure olur (HttpException).
     */
    suspend fun deleteCard(cardId: String): Result<List<CardUi>> = runCatching {
        val response = cardApi.remove(cardId)
        if (!response.isSuccessful) throw HttpException(response)
        cardApi.list().map { it.toUi() }
    }

    /** POST /rentals/{id}/pay — cüzdanla öder (bakiye yetersizse API 409 döner → Result.failure). */
    suspend fun payWithWallet(rentalId: String, discountCode: String?): Result<PaymentResultUi> =
        runCatching {
            rentalApi.pay(rentalId, PayRentalRequest(method = "WALLET", discountCode = discountCode))
                .toResult()
        }

    /** POST /rentals/{id}/pay — kayıtlı kartla (simüle) öder; [cardId] zorunlu. */
    suspend fun payWithCard(rentalId: String, cardId: String, discountCode: String?): Result<PaymentResultUi> =
        runCatching {
            rentalApi.pay(
                rentalId,
                PayRentalRequest(method = "CARD", cardId = cardId, discountCode = discountCode),
            ).toResult()
        }
}
