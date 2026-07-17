package com.turkcell.rencar.data.model

/**
 * İyzico Checkout Form akışı — UI/domain modelleri (decisions.md → "Katman Derinliği" mapper katmanı).
 * DTO'lar: [com.turkcell.rencar.data.remote.dto.CheckoutFormInitializeResponse],
 * [com.turkcell.rencar.data.remote.dto.IyzicoPaymentResponse].
 *
 * Ekran ham İyzico alanı (status string'leri, kart meta, fraud) tutmaz; yorum mapper'da yapılır.
 */

/**
 * Açılmış ödeme sayfası oturumu (POST /iyzico/checkout-form/initialize). [paymentPageUrl] WebView'a
 * yüklenir; [token] ödeme bitince sonucu sorgulamak için saklanır.
 */
data class IyzicoCheckoutUi(
    val token: String,
    val paymentPageUrl: String,
)

/**
 * Ödeme sayfası sonucunun yorumu (GET /iyzico/checkout-form/result/{token}).
 *
 * [isSuccess] yalnızca `paymentStatus == SUCCESS` iken true — 3DS ara durumları (INIT_THREEDS /
 * CALLBACK_THREEDS) tahsilatın tamamlandığı anlamına GELMEZ. [paymentId] başarılı ödemede dolu
 * olur ve POST /rentals/{id}/pay'e `iyzicoPaymentId` olarak geçilir.
 */
data class IyzicoVerificationUi(
    val isSuccess: Boolean,
    val paymentId: String?,
)
