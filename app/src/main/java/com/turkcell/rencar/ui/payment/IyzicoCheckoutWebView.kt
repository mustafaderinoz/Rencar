package com.turkcell.rencar.ui.payment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.turkcell.rencar.BuildConfig
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary

/** Marka mavisi — tema-bağımsız (bkz. PaymentScreen/ActiveRental). */
private val RencarBlue = LightPrimary

/**
 * İyzico'nun barındırdığı ortak ödeme sayfasını (Checkout Form) uygulama içinde gösteren tam ekran
 * katman. Kart bilgisi UYGULAMAYA GİRİLMEZ — kullanıcı doğrudan İyzico'nun sayfasına girer; bu yüzden
 * kart verisi hiçbir zaman istemcinin sorumluluğunda olmaz (PCI kapsamı dışı).
 *
 * Ödeme bitince İyzico tarayıcıyı sunucudaki callback adresine döndürür; WebView KENDİ KENDİNE
 * KAPANMAZ, bu yüzden yüklenen adresler dinlenip [onCallbackReached] ile akış sonlandırılır.
 *
 * Katmanın İÇİNDEKİ sayfa İyzico'ya (3D Secure adımında bankaya) aittir; görünümü bizim
 * kontrolümüzde değildir ve DEĞİŞTİRİLMEZ (decisions.md). Bu dosyada yalnız kendi çerçevemiz
 * (başlık, yükleniyor/hata durumu, kapatma onayı) biçimlendirilir.
 *
 * Sandbox test kartı: 5528790000000008 · 12/2030 · CVC 123 — 3D Secure SMS kodu: 283126.
 */
@Composable
fun IyzicoCheckoutOverlay(
    paymentPageUrl: String,
    onCallbackReached: () -> Unit,
    onClose: () -> Unit,
) {
    // Yanlışlıkla kapatıp yarım kalan ödemeyi önlemek için çıkış onayı sorulur (yalnız görünüm
    // durumu; uygulama state'i değil — ViewModel'e taşınmaz).
    var showExitConfirm by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            CheckoutTopBar(onClose = { showExitConfirm = true })
            // Yeni bir form oturumu (farklı URL) açıldığında WebView baştan kurulsun.
            key(paymentPageUrl) {
                CheckoutWebView(
                    paymentPageUrl = paymentPageUrl,
                    onCallbackReached = onCallbackReached,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    // Geri tuşu da ✕ ile aynı davranır: doğrudan kapatmaz, önce onay sorar.
    BackHandler(enabled = true) { showExitConfirm = true }

    if (showExitConfirm) {
        ExitConfirmDialog(
            onDismiss = { showExitConfirm = false },
            onConfirm = {
                showExitConfirm = false
                onClose()
            },
        )
    }
}

// ── Üst başlık: 🔒 "İyzico Güvenli Ödeme" + alt satır güvence metni + ✕ kapat ──
@Composable
private fun CheckoutTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(RencarBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Lock,
                contentDescription = null,
                tint = RencarBlue,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "İyzico Güvenli Ödeme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Kart bilgileriniz uygulamada tutulmaz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Close,
                contentDescription = "Ödemeyi kapat",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    )
}

/**
 * Ödeme sayfasını yükleyen WebView. JavaScript ve DOM storage İyzico'nun sayfası (ve 3D Secure
 * adımındaki banka sayfası) için zorunludur; sayfa yalnızca İyzico/banka kaynaklıdır.
 *
 * Sayfa yüklenene kadar WebView boş beyaz kalır (koyu temada da beyaz), bu yüzden üstü kendi
 * yükleniyor katmanımızla örtülür.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CheckoutWebView(
    paymentPageUrl: String,
    onCallbackReached: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // WebViewClient bir kez kurulur; güncel callback'i recomposition'a rağmen görebilmeli.
    val currentOnCallback by rememberUpdatedState(onCallbackReached)
    var isLoading by remember { mutableStateOf(true) }
    var hasLoadError by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient = object : WebViewClient() {
                        /** Callback yalnız bir kez bildirilir (sayfa birden çok kez başlayabilir). */
                        private var notified = false

                        private fun notifyIfCallback(url: String?) {
                            if (notified || url == null || !url.startsWith(CALLBACK_URL)) return
                            notified = true
                            currentOnCallback()
                        }

                        /**
                         * İyzico callback'e **POST** ile döner ve `shouldOverrideUrlLoading` POST
                         * isteklerinde ÇAĞRILMAZ; bu yüzden adres dinlemesi burada yapılır.
                         */
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            isLoading = true
                            hasLoadError = false
                            notifyIfCallback(url)
                            super.onPageStarted(view, url, favicon)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                            // Yalnız debug: sandbox 3DS simülatörünün ham görünümünü düzeltir.
                            if (BuildConfig.DEBUG) {
                                view?.evaluateJavascript(SANDBOX_3DS_RESTYLE_JS, null)
                            }
                            super.onPageFinished(view, url)
                        }

                        /**
                         * Yalnız ana çerçeve hataları gösterilir: sayfadaki bir görselin/analitik
                         * isteğinin düşmesi ödemeyi engellemez, tam ekran hata göstermek yanıltıcı olur.
                         */
                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasLoadError = true
                            }
                            super.onReceivedError(view, request, error)
                        }
                    }
                    webView = this
                    loadUrl(paymentPageUrl)
                }
            },
            // WebView'ı sızdırmamak için ekrandan ayrılırken yüklemeyi durdur.
            onRelease = { released ->
                released.stopLoading()
                released.destroy()
                webView = null
            },
        )

        when {
            hasLoadError -> LoadErrorState(
                onRetry = {
                    hasLoadError = false
                    isLoading = true
                    webView?.reload()
                },
            )
            isLoading -> LoadingState()
        }
    }
}

// ── Sayfa yüklenirken: boş beyaz WebView'ı örten kendi yükleniyor katmanımız ──
@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Güvenli ödeme sayfası açılıyor…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Sayfa yüklenemedi: WebView'ın kendi hata sayfasını örter + tekrar dene ──
@Composable
private fun LoadErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = RencarIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(32.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Ödeme sayfası yüklenemedi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "İnternet bağlantınızı kontrol edip tekrar deneyin. Tutar tahsil edilmedi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RencarBlue, contentColor = Color.White),
        ) {
            Text(text = "Tekrar dene", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Ödemeden vazgeçme onayı (Dialog) — Kart Sil onayıyla aynı kalıp ──
@Composable
private fun ExitConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(22.dp),
        ) {
            Text(
                text = "Ödemeden vazgeç",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Güvenli ödeme sayfası kapatılsın mı? Ödemeyi tamamlamadıysanız " +
                    "kartınızdan tutar çekilmez.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Ödemeye dön", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(text = "Vazgeç", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * İyzico'nun ödeme sonrası döndüğü sunucu adresi. Bu adres görüldüğü an ödeme akışı bitmiştir
 * (başarılı ya da başarısız); sonuç ayrıca token ile sorgulanır.
 */
private val CALLBACK_URL: String =
    BuildConfig.BASE_URL.trimEnd('/') + "/iyzico/checkout-form/callback"

/**
 * İyzico SANDBOX 3D Secure simülatörünün (biçimlendirilmemiş "Sms Code + Submit/Cancel" sayfası)
 * görünümünü uygulamanın diline yaklaştıran stil enjeksiyonu.
 *
 * **Yalnız `BuildConfig.DEBUG`'da çağrılır** (bkz. `onPageFinished`). Gerekçe: production'da bu
 * adımda GERÇEK bankanın 3DS sayfası açılır ve bir bankanın güvenlik sayfası biçimlendirilmez.
 *
 * **Kendi kendini hedefler:** sayfanın HTML'i bize ait olmadığından CSS sınıf adları VARSAYILMAZ
 * (§2.2). Script yalnızca metni tam olarak "submit" ve "cancel" olan iki eylem öğesi bulursa çalışır;
 * bulamazsa (ör. İyzico'nun kendi ödeme formu, gerçek banka sayfası) hiçbir şey yapmadan çıkar.
 *
 * **Form bağı korunarak taşınır:** iki buton yan yana dizilebilmek için tek bir flex satırına alınır.
 * Submit/Cancel ayrı `<form>`'larda olabileceğinden, taşımadan ÖNCE her butona HTML5 `form`
 * attribute'ü yazılır (gerekirse forma id üretilir) — böylece buton formun dışına çıksa da
 * gönderim bağı kopmaz. Taşıma `try/catch` içindedir: başarısız olursa butonlar yerinde kalır ve
 * alt alta (yedek düzen) görünür, ödeme akışı yine çalışır.
 *
 * İyzico bu sayfanın metinlerini değiştirirse enjeksiyon sessizce devre dışı kalır — sayfa yine
 * çalışır, sadece ham görünür.
 */
private const val SANDBOX_3DS_RESTYLE_JS = """
(function () {
  if (document.getElementById('rencar-3ds-style')) return;

  var nodes = document.querySelectorAll('button, input[type=submit], input[type=button], a');
  var submit = null, cancel = null;
  for (var i = 0; i < nodes.length; i++) {
    var el = nodes[i];
    var label = ((el.innerText || el.value || '') + '').trim().toLowerCase();
    if (!submit && label === 'submit') submit = el;
    if (!cancel && label === 'cancel') cancel = el;
  }
  if (!submit || !cancel) return;

  /* Renkler uygulamanın tema paletinden (ui/theme/Color.kt): uygulama zemini #F5F6F8, kart/alan
     zemini #FFFFFF, çerçeve #C4C6D0, birincil metin #191C20, ikincil metin #44474F, marka #1A6BF0.
     Sayfa düz beyazken alan ve Cancel zemine karışıyordu; zemin griye alınıp ikisi beyaz karta
     dönüştürülerek öne çıkarıldı (uygulamadaki SectionCard kalıbı). */
  /* Kod alanının seçicisi. 'input[type=text]' YETMEZ: type attribute'ü olmayan bir <input> de metin
     alanıdır (varsayılan type 'text'tir) ve bu sayfadaki alan tam olarak öyle. Eski seçici hiç
     eşleşmediği için alan biçimlenmemiş, tarayıcı varsayılan genişliğinde (~200px, sola yaslı) kalmış
     ve sütun hizasını bozmuştu. Bu yüzden tersten tanımlanır: buton/kutu türleri dışlanır, geriye
     kalan her <input> metin alanıdır — sayfanın 'type' yazmış olmasına GÜVENİLMEZ. */
  var FIELD = 'input:not([type=submit]):not([type=button]):not([type=reset])' +
    ':not([type=checkbox]):not([type=radio]):not([type=hidden]):not([type=image])';

  var css =
    /* Zemin <html>'e verilir: <body> artık ekranı değil, içerik SÜTUNUNU temsil ediyor (aşağıya bkz.)
       ve dar olduğu için zemini kaplayamaz. */
    'html{height:100%!important;background:#F5F6F8!important}' +
    /* <body> = içerik sütunu. 'max-width' + 'margin:0 auto' ile ekranda yatay olarak ortalanır; alan
       ve butonlar bu sütunu doldurduğu için kenarları AYNI yere düşer — düzeni derli toplu gösteren
       şey bu tek hizadır. Paragraf da aynı sütunda ortalanır ama satırları kelime kelime sarıldığı
       için en uzun satırı kenara birkaç px kala biter; bu fark kapatılamaz (metin İyzico'nun) ve
       yatay dolgu 26px seçilerek küçültülmüştür — daha da daraltmak satırı erken sardırma riski taşır.
       Sütunu <body> olmak zorunda; sayfanın kendi sarmalayıcılarına genişlik veremeyiz, çünkü onlara
       aşağıdaki 'unconstrain' inline 'width:auto!important' yazıyor ve inline stil stylesheet'i yener.
       <body> o döngünün dışında (döngü body'ye gelince durur), dolayısıyla ezilmeyen tek yer burası.
       Sütun içindeki sarmalayıcılar 'width:auto' + varsayılan 'align-items:stretch' ile sütunu
       doldurur; bu yüzden align-items DEĞİŞTİRİLMEZ ('center' onları içerik genişliğine büzer).
       Dikeyde ortalanmaz, üstten konumlanır: blok üstten 15vh aşağıda başlar. Sabit px yerine vh,
       çünkü konum ekran yüksekliğiyle orantılı olmalı. Klavye açıldığında içerik yerinde kalır ve
       gerekirse sayfa normal biçimde kayar — ortalamada gereken 'safe center' korumasına gerek yok. */
    'body{font-family:-apple-system,"Segoe UI",Roboto,system-ui,sans-serif!important;' +
      'box-sizing:border-box!important;min-height:100vh!important;display:flex!important;' +
      'flex-direction:column!important;justify-content:flex-start!important;' +
      'max-width:360px!important;margin:0 auto!important;' +
      'padding:15vh 26px 32px!important;background:transparent!important;' +
      'color:#191C20!important;line-height:1.5!important;text-align:center!important;' +
      '-webkit-text-size-adjust:100%!important}' +
    /* text-align gövdeden miras kalır ama sayfanın kendi sarmalayıcısı 'left' diyorsa ezerdi;
       bu yüzden ortalama öğenin kendisinde de açıkça belirtilir. */
    'p{font-size:15px!important;color:#44474F!important;margin:0 0 24px!important;' +
      'line-height:1.55!important;text-align:center!important}' +
    /* Alan üstü etiket: küçük, harf aralıklı ve büyük harf — form etiketinin alışılmış "üst başlık"
       görünümü, paragrafla karışmasını önler. */
    'label{display:block!important;font-size:12px!important;font-weight:600!important;' +
      'text-transform:uppercase!important;letter-spacing:.6px!important;' +
      'color:#6F7278!important;margin:0 0 10px!important;text-align:center!important}' +
    /* Tek seferlik kod alanı: iri, yarı kalın ve harf aralıklı — kodun okunup doğrulanması kolay olsun
       (girilen rakamları tek tek ayırır). Metin ortalı: kodun alışılmış hizası.
       Punto 16px'in altına inmemeli; iner ise mobil tarayıcı odakta sayfayı yakınlaştırır.
       'appearance:none': WebView metin alanlarına yerleşik bir çerçeve/iç gölge çizer ve bizim
       çerçevemizin üstüne biner. */
    FIELD + '{width:100%!important;box-sizing:border-box!important;padding:11px 14px!important;' +
      'font-size:18px!important;font-weight:600!important;letter-spacing:2px!important;' +
      '-webkit-appearance:none!important;appearance:none!important;' +
      'border:1px solid #C4C6D0!important;border-radius:12px!important;' +
      'background:#FFFFFF!important;background-image:none!important;' +
      'color:#191C20!important;outline:none!important;margin:0!important;' +
      'text-align:center!important;' +
      'box-shadow:0 1px 2px rgba(25,28,32,.06)!important}' +
    /* Yer tutucu, girilen koddan ayrışmalı: normal punto/kalınlık, aralıksız, soluk. */
    FIELD + '::-webkit-input-placeholder{color:#9A9DA3!important;font-size:15px!important;' +
      'font-weight:400!important;letter-spacing:0!important}' +
    FIELD + '::placeholder{color:#9A9DA3!important;font-size:15px!important;' +
      'font-weight:400!important;letter-spacing:0!important}' +
    FIELD + ':focus{border-color:#1A6BF0!important;' +
      'box-shadow:0 0 0 3px rgba(26,107,240,.15)!important}' +
    /* Taban: taşıma başarısız olursa yedek düzen — alt alta, tam genişlik.
       Yükseklik alanınkinden belirgin biçimde az: kod alanı bu ekranın asıl işi, butonlar ona eşlik
       eder — eşit irilikte olurlarsa ikisi de öne çıkmak için yarışır. */
    '.rencar-btn{display:block!important;width:100%!important;box-sizing:border-box!important;' +
      'padding:10px 12px!important;margin:8px 0 0!important;font-size:14px!important;' +
      'font-weight:600!important;border-radius:10px!important;border:none!important;' +
      'cursor:pointer!important;text-align:center!important;text-decoration:none!important;' +
      'line-height:1.2!important;float:none!important;box-shadow:none!important;' +
      /* WebView, <button>/<input type=button>'a yerleşik bir görünüm (gri degrade + kendi çerçevesi)
         çizer; 'background' bunu her zaman bastırmaz ve zeminimizin altından sızar. Cancel'ın solunda
         görünen gri bunun izi. 'appearance:none' yerleşik çizimi tamamen kapatır; degradeyi ayrıca
         'background-image:none' ile siliyoruz, çünkü kısa 'background' yazımı onu her yerde
         sıfırlamıyor. */
      '-webkit-appearance:none!important;appearance:none!important;' +
      'background-image:none!important;background-clip:padding-box!important}' +
    /* Taşıma başarılıysa: tek satırda, eşit genişlikte. Butonlar arası boşluk, butonların kendi iç
       dolgusundan (10px) belirgin biçimde büyük olmalı; aksi halde ikisi tek bir çubuk gibi okunuyor.
       Alanla arasındaki boşluk 'margin-top' DEĞİL 'padding-top' ile verilir: satır, sarmalayıcısının
       ilk çocuğu olabilir ve o durumda üst margin sarmalayıcının dışına taşar (margin collapsing) —
       boşluk alanla butonların arasında değil, bambaşka bir yerde oluşurdu. Padding taşmaz. */
    '.rencar-actions{display:flex!important;gap:16px!important;' +
      'margin:0!important;padding:20px 0 0!important}' +
    '.rencar-actions .rencar-btn{flex:1 1 0!important;width:auto!important;min-width:0!important;' +
      'margin:0!important}' +
    '.rencar-btn-primary{background:#1A6BF0!important;color:#FFFFFF!important;' +
      'box-shadow:0 1px 2px rgba(26,107,240,.24)!important}' +
    '.rencar-btn-primary:active{background:#1557C4!important}' +
    /* Sonra tanımlı: taban .rencar-btn'in box-shadow:none'ını bilinçli olarak geçersiz kılar. */
    '.rencar-btn-ghost{background:#FFFFFF!important;color:#191C20!important;' +
      'border:1px solid #C4C6D0!important;box-shadow:0 1px 2px rgba(25,28,32,.06)!important}' +
    '.rencar-btn-ghost:active{background:#EFF1F4!important}';

  var style = document.createElement('style');
  style.id = 'rencar-3ds-style';
  style.textContent = css;
  document.head.appendChild(style);

  submit.classList.add('rencar-btn', 'rencar-btn-primary');
  cancel.classList.add('rencar-btn', 'rencar-btn-ghost');

  // İki butonu tek flex satırına al. Farklı <form>'larda olabilirler; taşımadan önce HTML5 'form'
  // attribute'ü ile bağlarını sabitle, böylece form dışına çıksalar da gönderim bozulmaz.
  try {
    var seq = 0;
    var bind = function (el) {
      if (!('form' in el)) return; // <a> gibi form-ilişkisiz öğe: taşımak zaten güvenli
      var f = el.form;
      if (!f) return;
      if (!f.id) f.id = 'rencar-form-' + (++seq);
      el.setAttribute('form', f.id);
    };
    bind(submit);
    bind(cancel);

    var row = document.createElement('div');
    row.className = 'rencar-actions';
    submit.parentNode.insertBefore(row, submit);
    row.appendChild(cancel); // ikincil eylem solda
    row.appendChild(submit); // birincil eylem sağda
  } catch (e) {
    // Taşınamadıysa butonlar yerinde kalır: taban stille alt alta görünür, sayfa çalışmaya devam eder.
  }

  // Sayfanın KENDİ sarmalayıcıları içeriği dar ve sabit bir sütuna sıkıştırıyor (alan ekranın
  // ortasında ~200px kalıyor). '.rencar-btn{width:100%}' gibi kurallar o sarmalayıcının %100'ünü
  // aldığından ekranı kaplamıyor. Öğeden <body>'ye kadar yürüyüp genişlik/kenar kısıtlarını
  // kaldırırız: içerik body dolgusuna kadar açılır ve dolgular eşit olduğu için yatayda da
  // kendiliğinden ortalanır. Sarmalayıcıların ne olduğunu VARSAYMAYIZ — zincir neyse etkisizleşir.
  //
  // Dikey kısıtlar da aynı sebeple sıfırlanır: zincirdeki bir sarmalayıcının üst margin/padding'i
  // bloğu body'nin 15vh'sinin ALTINA iter, yani konumu sessizce kaçırırdı. Sıfırlandığında bloğun
  // nerede başladığını tek bir yer belirler: body'nin padding-top'u.
  try {
    var unconstrain = function (el) {
      var n = el && el.parentElement;
      while (n && n !== document.body && n.nodeType === 1) {
        // Eylem satırı BİZİM öğemiz ve butonlar onun içine taşındığı için bu zincire dahil oluyor.
        // Döngünün işi sayfanın kendi sarmalayıcılarını etkisizleştirmek; kendi ölçülerimizi
        // (üst boşluk, gap) silmemeli — atlanır, yürüyüş yukarı devam eder.
        if (n.classList.contains('rencar-actions')) { n = n.parentElement; continue; }
        n.style.setProperty('width', 'auto', 'important');
        n.style.setProperty('min-width', '0', 'important');
        n.style.setProperty('max-width', 'none', 'important');
        n.style.setProperty('float', 'none', 'important');
        n.style.setProperty('margin-left', '0', 'important');
        n.style.setProperty('margin-right', '0', 'important');
        n.style.setProperty('padding-left', '0', 'important');
        n.style.setProperty('padding-right', '0', 'important');
        n.style.setProperty('margin-top', '0', 'important');
        n.style.setProperty('margin-bottom', '0', 'important');
        n.style.setProperty('padding-top', '0', 'important');
        n.style.setProperty('padding-bottom', '0', 'important');
        // Blok olmayan sarmalayıcı (inline-block, tablo hücresi…) genişliğini İÇERİĞİNDEN alır:
        // 'width:auto' onu açmaz, içine büzer — ve içindeki 'width:100%' o dar kutunun %100'ü olur.
        // Yalnız bu görünüm türleri bloğa çevrilir; zaten blok olanlara dokunulmaz.
        var d = window.getComputedStyle(n).display;
        if (d === 'inline' || d === 'inline-block' || d === 'table' ||
            d === 'inline-table' || d === 'table-cell' || d === 'table-row') {
          n.style.setProperty('display', 'block', 'important');
        }
        n = n.parentElement;
      }
    };
    unconstrain(submit);
    unconstrain(cancel);
    unconstrain(document.querySelector(FIELD));
  } catch (e) {
    // Kısıt kaldırılamadıysa içerik dar kalır ama sayfa çalışmaya devam eder.
  }
})();
"""
