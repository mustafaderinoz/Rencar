package com.turkcell.rencar.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * İyzico Checkout Form (ortak ödeme sayfası) DTO'ları — openapi.json şemalarıyla birebir (tag: Iyzico).
 *
 * Akış (decisions.md → "İyzico ile Ödeme"):
 * 1. POST /iyzico/checkout-form/initialize → [InitializeCheckoutFormRequest] → [CheckoutFormInitializeResponse]
 *    (token + paymentPageUrl; sayfa WebView'da açılır, kart bilgisi UYGULAMAYA GİRİLMEZ).
 * 2. Kullanıcı İyzico sayfasında öder; İyzico tarayıcıyı sunucudaki
 *    POST /iyzico/checkout-form/callback adresine döndürür (WebView bunu yakalayıp kapanır).
 * 3. GET /iyzico/checkout-form/result/{token} → [IyzicoPaymentResponse] (paymentStatus + paymentId).
 * 4. POST /rentals/{id}/pay (method=IYZICO + iyzicoPaymentId) → kiralama PAID işaretlenir.
 *
 * DTO'lar UI'a doğrudan verilmez; mapper katmanında modele çevrilir (decisions.md → "Katman Derinliği").
 */

/**
 * POST /iyzico/checkout-form/initialize gövdesi (InitializeCheckoutFormDto).
 *
 * [price] tahsil edilecek tutar (TL) — API 1–100000 aralığını kabul eder.
 * [basketId] kiralama ödemesinde **`rental-<kiralamaId>`** olmak ZORUNDADIR: POST /rentals/{id}/pay
 * (IYZICO) doğrulaması bu değeri arar. [enabledInstallments] formda sunulacak taksit seçenekleri;
 * varsayılan tek çekim ([1]).
 *
 * `buyer` alanı GÖNDERİLMEZ: sunucu alıcı bilgilerini giriş yapmış kullanıcının kaydından doldurur
 * (AGENTS §2.2 — adres/TCKN gibi elimizde olmayan veri uydurulmaz).
 */
@Serializable
data class InitializeCheckoutFormRequest(
    val price: Double,
    val description: String,
    val basketId: String,
    val enabledInstallments: List<Int> = listOf(1),
)

/**
 * POST /iyzico/checkout-form/initialize 201 cevabı (CheckoutFormInitializeResponseDto).
 *
 * [status] İyzico işlem durumu ("success" | "failure"). [token] form oturumu token'ı — sonuç bununla
 * sorgulanır. [paymentPageUrl] İyzico'nun barındırdığı hazır ödeme sayfası (WebView'a yüklenir);
 * şemada zorunlu değildir, bu yüzden nullable.
 *
 * [checkoutFormContent] formu kendi sayfamıza gömmek için script bloğudur — mobilde hazır sayfa
 * (paymentPageUrl) kullanıldığından okunmaz, sözleşme bütünlüğü için tutulur.
 */
@Serializable
data class CheckoutFormInitializeResponse(
    val status: String,
    val token: String,
    val tokenExpireTime: Int? = null,
    val paymentPageUrl: String? = null,
    val checkoutFormContent: String? = null,
)

/**
 * GET /iyzico/checkout-form/result/{token} 200 cevabı (IyzicoPaymentResponseDto) — ödemenin
 * İyzico'daki son durumu.
 *
 * [paymentStatus] SUCCESS | FAILURE | INIT_THREEDS | CALLBACK_THREEDS; tahsilatın tamamlandığı
 * YALNIZCA `SUCCESS` ile anlaşılır. [paymentId] POST /rentals/{id}/pay'e `iyzicoPaymentId` olarak
 * verilir. Diğer alanlar (kart meta, fraud, iade kırılımları) bu akışta okunmaz; şema bütünlüğü
 * için tanımlıdır ve tümü nullable-default'tur (şemada yalnız [status] zorunlu).
 */
@Serializable
data class IyzicoPaymentResponse(
    val status: String,
    val paymentId: String? = null,
    val conversationId: String? = null,
    val price: Double? = null,
    val paidPrice: Double? = null,
    val currency: String? = null,
    val installment: Int? = null,
    val paymentStatus: String? = null,
    val token: String? = null,
    val fraudStatus: Int? = null,
    val binNumber: String? = null,
    val lastFourDigits: String? = null,
    val cardType: String? = null,
    val cardAssociation: String? = null,
    val cardFamily: String? = null,
    val paymentTransactionIds: List<String> = emptyList(),
)
