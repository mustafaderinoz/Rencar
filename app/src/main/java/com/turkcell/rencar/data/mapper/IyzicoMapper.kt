package com.turkcell.rencar.data.mapper

import com.turkcell.rencar.data.model.IyzicoCheckoutUi
import com.turkcell.rencar.data.model.IyzicoVerificationUi
import com.turkcell.rencar.data.remote.dto.CheckoutFormInitializeResponse
import com.turkcell.rencar.data.remote.dto.IyzicoPaymentResponse

/**
 * İyzico DTO → UI modeli dönüşümleri (ayrı mapper katmanı; decisions.md → "Katman Derinliği").
 * İyzico'nun string durum kodlarının yorumu burada emilir; ViewModel/UI "SUCCESS" gibi ham
 * değerlerle uğraşmaz ve şema değişikliği yalnızca bu katmanda karşılanır.
 */

/** İyzico'nun tahsilatı tamamlandı saydığı tek `paymentStatus` değeri. */
private const val PAYMENT_STATUS_SUCCESS = "SUCCESS"

/** İyzico isteğinin kabul edildiğini gösteren `status` değeri. */
private const val STATUS_SUCCESS = "success"

/**
 * POST /iyzico/checkout-form/initialize → WebView'da açılacak oturum.
 *
 * `status` "success" değilse veya `paymentPageUrl` boşsa oturum kullanılamaz: WebView'a yükleyecek
 * bir adres yoktur. Bu durumda null döner; çağıran (repository) bunu hataya çevirir.
 */
fun CheckoutFormInitializeResponse.toCheckoutOrNull(): IyzicoCheckoutUi? {
    val url = paymentPageUrl
    if (!status.equals(STATUS_SUCCESS, ignoreCase = true) || url.isNullOrBlank()) return null
    return IyzicoCheckoutUi(token = token, paymentPageUrl = url)
}

/**
 * GET /iyzico/checkout-form/result/{token} → ödeme doğrulaması. Başarı için hem `paymentStatus`
 * SUCCESS olmalı hem de [IyzicoPaymentResponse.paymentId] dolu gelmeli: ödeme id'si olmadan
 * POST /rentals/{id}/pay çağrılamaz (IYZICO yönteminde zorunlu alan).
 */
fun IyzicoPaymentResponse.toVerification(): IyzicoVerificationUi {
    val succeeded = paymentStatus.equals(PAYMENT_STATUS_SUCCESS, ignoreCase = true) &&
        !paymentId.isNullOrBlank()
    return IyzicoVerificationUi(
        isSuccess = succeeded,
        paymentId = paymentId?.takeIf { it.isNotBlank() },
    )
}
