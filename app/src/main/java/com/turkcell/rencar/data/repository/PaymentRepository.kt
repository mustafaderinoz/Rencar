package com.turkcell.rencar.data.repository

import com.turkcell.rencar.data.mapper.toBalance
import com.turkcell.rencar.data.mapper.toCheckoutOrNull
import com.turkcell.rencar.data.mapper.toPaymentReceipt
import com.turkcell.rencar.data.mapper.toResult
import com.turkcell.rencar.data.mapper.toUi
import com.turkcell.rencar.data.mapper.toVerification
import com.turkcell.rencar.data.model.CardUi
import com.turkcell.rencar.data.model.IyzicoCheckoutUi
import com.turkcell.rencar.data.model.IyzicoVerificationUi
import com.turkcell.rencar.data.model.PaymentReceiptUi
import com.turkcell.rencar.data.model.PaymentResultUi
import com.turkcell.rencar.data.remote.api.CardApi
import com.turkcell.rencar.data.remote.api.IyzicoApi
import com.turkcell.rencar.data.remote.api.RentalApi
import com.turkcell.rencar.data.remote.api.WalletApi
import com.turkcell.rencar.data.remote.dto.CreateCardRequest
import com.turkcell.rencar.data.remote.dto.InitializeCheckoutFormRequest
import com.turkcell.rencar.data.remote.dto.PayRentalRequest
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException

/**
 * Ödeme iş akışı (karar: decisions.md → data + repository, ayrı mapper katmanı). ViewModel →
 * PaymentRepository → CardApi/WalletApi/RentalApi/IyzicoApi. Cüzdan/kart ödeme domain'i
 * RentalRepository'den ayrı tutulur (tek sorumluluk). Hata yönetimi Result ile çağırana taşınır;
 * mesaj eşlemesi ViewModel'de.
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val cardApi: CardApi,
    private val walletApi: WalletApi,
    private val rentalApi: RentalApi,
    private val iyzicoApi: IyzicoApi,
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

    // ── İyzico Checkout Form (gerçek tahsilat) — üç adım: initialize → verify → pay ──

    /**
     * 1/3 — POST /iyzico/checkout-form/initialize: İyzico'nun ortak ödeme sayfasını açar.
     *
     * [amount] dökümdeki toplam tutardır. `basketId` sözleşmesi ([BASKET_ID_PREFIX] + kiralama id'si)
     * burada kurulur: POST /rentals/{id}/pay doğrulaması bu değeri arar, bu yüzden UI'ın bilmesi
     * gereken bir ayrıntı değildir.
     *
     * İyzico isteği reddederse (status != success) veya sayfa adresi boş gelirse — WebView'a
     * yüklenecek adres yoktur — [IllegalStateException] ile Result.failure olur.
     */
    suspend fun startIyzicoCheckout(rentalId: String, amount: Double): Result<IyzicoCheckoutUi> =
        runCatching {
            iyzicoApi.initializeCheckoutForm(
                InitializeCheckoutFormRequest(
                    price = amount,
                    description = CHECKOUT_DESCRIPTION,
                    basketId = "$BASKET_ID_PREFIX$rentalId",
                ),
            ).toCheckoutOrNull() ?: error("İyzico ödeme sayfası açılamadı.")
        }

    /**
     * 2/3 — GET /iyzico/checkout-form/result/{token}: WebView callback adresine döndükten sonra
     * ödemenin İyzico'daki gerçek durumunu sorgular. Başarısız ödeme de 200 döner
     * ([IyzicoVerificationUi.isSuccess] false olur); yalnız ağ/HTTP hatası Result.failure'dır.
     */
    suspend fun verifyIyzicoCheckout(token: String): Result<IyzicoVerificationUi> =
        runCatching { iyzicoApi.getCheckoutFormResult(token).toVerification() }

    /**
     * 3/3 — POST /rentals/{id}/pay: doğrulanmış İyzico ödemesini kiralamaya işler (PAID).
     * Sunucu tutarı ve `rental-<id>` basketId'sini İyzico'dan yeniden doğrular. İndirim kodu bu
     * yöntemde gönderilemez (API 400 döner), bu yüzden parametresi de yoktur.
     */
    suspend fun payWithIyzico(rentalId: String, iyzicoPaymentId: String): Result<PaymentResultUi> =
        runCatching {
            rentalApi.pay(
                rentalId,
                PayRentalRequest(method = "IYZICO", iyzicoPaymentId = iyzicoPaymentId),
            ).toResult()
        }

    private companion object {
        /** POST /rentals/{id}/pay (IYZICO) doğrulamasının aradığı sepet kimliği öneki. */
        const val BASKET_ID_PREFIX = "rental-"

        /** İyzico sepet kaleminde görünecek açıklama. */
        const val CHECKOUT_DESCRIPTION = "RenCar yolculuk ödemesi"
    }
}
