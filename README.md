<div align="center">

# 🚙 RenCar

**Dakikalık & Saatlik & Günlük Araç Kiralama Uygulaması**

Kotlin ve Jetpack Compose ile yazılmış; haritadan araç bulma, ehliyet + selfie ile kimlik doğrulama, canlı yolculuk takibi ve yapay zekâ destekli araç önerisi içeren modern bir Android araç kiralama uygulaması.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white)](#)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose%20BOM-2026.02.01-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange)](#)
[![Architecture](https://img.shields.io/badge/Architecture-MVI-blueviolet)](#)
[![Gemini](https://img.shields.io/badge/AI-Google%20Gemini-8E75B2?logo=googlegemini&logoColor=white)](#)

</div>

---

## 📖 Uygulama Hakkında

**RenCar**, kullanıcıların haritadaki müsait araçları görüp anında kiralayabildiği bir **Araç Kiralama** uygulamasıdır.

Kullanıcı; parolasız OTP ile giriş yapar, ehliyetini ve selfie'sini yükleyerek hesabını doğrular, harita üzerinden aracını seçer, dilerse **doğal dille** ("2000 TL altı otomatik vitesli lüks araç") yapay zekâya öneri sorar, aracı **15 dakika ücretsiz rezerve eder**, teslim almadan önce **hasar fotoğraflarını** çeker, yolculuk boyunca **canlı konumunu** takip eder ve bitişte kayıtlı kartı, **iyzico veya cüzdan bakiyesiyle** ödemesini yapar.

---

## ✨ Öne Çıkan Özellikler

| | Özellik | Açıklama |
|:---:|---|---|
| 🔐 | **Parolasız Kimlik Doğrulama** | Telefon numarası + OTP akışı; kayıtsız numara otomatik olarak kayıt ekranına yönlendirilir |
| 🪪 | **Ehliyet Doğrulama** | Ehliyet ön/arka yüz çekimi, backend onayına kadar `UNDER_REVIEW` bekleme ekranı |
| 🤳 | **Canlı Selfie Doğrulama** | CameraX + **ML Kit Face Detection** ile yüz merkezleme analizi — kullanıcı doğru konumlandığında otomatik çekim |
| 🗺️ | **Harita & Araç Keşfi** | **MapLibre** tabanlı harita, canlı araç işaretçileri ve alt kart ile hızlı araç önizleme |
| 🤖 | **AI Araç Önerisi** | **Google Gemini** ile doğal dil sorgusundan araç filtreleme (bütçe, segment, kasa tipi, vites) |
| 📋 | **Araç Detayı & Rezervasyon** | Fiyat, yakıt, koltuk, segment bilgileri + **dakikalık / saatlik / günlük** plan seçimi |
| ⏱️ | **15 dk Ücretsiz Tutma & İptal** | Rezervasyon geri sayımı fotoğraf ekranında akar; kullanıcı dilediği an **"Rezervasyonu İptal Et"** ile aracı serbest bırakır |
| 📸 | **Teslim Alma Fotoğrafları** | Kiralama öncesi araç durum tespiti; 4 kare yerelde tutulur, "Kiralamayı Başlat" anında sıkıştırılıp yüklenir |
| 📍 | **Canlı Yolculuk Takibi** | **Socket.IO** (`/ws/locations`) üzerinden aracın anlık konumu haritada gerçek zamanlı |
| 🚙 | **Teslim Etme Fotoğrafları** | Yolculuk bitişinde, ödemeden önce araç teslim durumu kaydı |
| 💳 | **Ödeme** | Ücret dökümü, kayıtlı kartlar, indirim kodu ve **Iyzico** entegrasyonu ile ödeme |
| 👛 | **Cüzdan** | Bakiye yükleme, görüntüleme ve bakiye ile ödeme |
| 🕓 | **Kiralama Geçmişi** | Geçmiş yolculuklar ve detayları |
| 👤 | **Profil** | Kullanıcı bilgileri, ehliyet durumu ve oturum kapatma |
| 🌗 | **Uygulama İçi Koyu/Açık Tema** | Profil başlığındaki tek butonla anında tema değişimi; tercih DataStore'da kalıcı, seçim yapılmadıysa sistem teması takip edilir |
| 🔄 | **Otomatik Oturum Yenileme** | `AuthInterceptor` + `TokenAuthenticator` + `SessionManager` ile şeffaf token yenileme; DataStore'da güvenli saklama |



## 📱 Uygulama İçi Ekran Görüntüleri

### 🚀 Karşılama & Giriş

| Onboarding | OTP Doğrulama | Kayıt Ol |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/3ca8b7c0-22fb-4688-be3c-4c3fc8ff3142" width="245px" alt="Onboarding Ekranı" /> | <img src="https://github.com/user-attachments/assets/eb3d52f4-7eda-4b07-a86d-7d860c3a7df9" width="245px" alt="OTP Ekranı" /> | <img src="https://github.com/user-attachments/assets/861487f8-053a-4037-8cf7-3b59c6f3f13d" width="245px" alt="Kayıt Ol" /> |

### 🪪 Ehliyet & Selfie Doğrulama

| Ehliyet Doğrulama | Selfie Doğrulama | İnceleme Bekleniyor |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/6cfd6f79-1cd7-4d72-bc9e-35103d964f75" width="245px" alt="Ehliyet Doğrulama Ekranı" /> | <img src="https://github.com/user-attachments/assets/f4050ed6-f65e-4152-8132-774eb24c8b47" width="245px" alt="Selfie Doğrulama Ekranı" /> | <img src="https://github.com/user-attachments/assets/71bc020d-ef9e-4723-a03a-6dbe269e2017" width="245px" alt="İnceleme Bekleniyor" /> |

### 🏠 Ana Sayfa & Harita

| Ana Sayfa | Harita Ekranı | Araç Önizleme Kartı |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/45d70927-2563-4c2a-9b2d-67fcdd61b9b7" width="245px" alt="Ana Sayfa Ekranı" /> | <img src="https://github.com/user-attachments/assets/fa329de8-58ec-412c-9812-ab015be8feeb" width="245px" alt="Harita Ekranı" /> | <img src="https://github.com/user-attachments/assets/ac1e38ed-62e7-4e93-a3ab-ebcf3b454581" width="245px" alt="Araç Önizleme" /> |

### 🤖 Yapay Zekâ Destekli Araç Önerisi

> Kullanıcı doğal dille ne istediğini yazar, Gemini uygun araçları haritada filtreler.

| Sorgu Girişi | Önerilen Araçlar |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/77152aa2-6b65-4f6a-a5ca-8d87baaa1cd9" width="245px" alt="Sorgu Girişi Ekranı" /> | <img src="https://github.com/user-attachments/assets/99ac109d-b16b-45e9-85dc-450d1e916b75" width="245px" alt="Önerilen Araçlar Ekranı" /> |

### 📋 Rezervasyon & Teslim Alma

| Rezervasyon Onayı | Fotoğraf Yükleme | Kilitli Yolculuk Ekranı |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/d21a56cd-8fb2-4311-a75a-cd175b99c238" width="245px" alt="Rezervasyon Onayı Ekranı" /> | <img src="https://github.com/user-attachments/assets/baf18146-931c-4a1a-9744-92497883c21d" width="245px" alt="Fotoğraf Yükleme Ekranı" /> | <img src="https://github.com/user-attachments/assets/612d5283-3e5e-4eff-a427-e5c0e6b4ce1f" width="245px" alt="Kilitli Yolculuk Ekranı" /> |

### 📍 Aktif Yolculuk & Araç Teslim Etme

| Canlı Konum Takibi | Teslim Fotoğrafı Çekimi | Yolculuk Özeti |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/23026a14-1db3-4148-a02c-76e17d699561" width="245px" alt="Canlı Konum Takibi " /> | <img src="https://github.com/user-attachments/assets/151c021e-5514-40ce-b97c-e28926862b2a" width="245px" alt="Teslim Fotoğraf Ekranı" /> | <img src="https://github.com/user-attachments/assets/4b650002-f6f0-40ec-ae6e-d6c352672bcb" width="245px" alt=" Yolculuk Özeti Ekranı" /> |

### 💳 Ödeme

| Kayıtlı Kartlar | İyzico ile Ödeme | Ödeme Başarılı |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/b7d29450-f8cd-4c69-89de-85695d655658" width="245px" alt="Kayıtlı Kartlar " /> | <img src="https://github.com/user-attachments/assets/3fa54f20-4537-4961-93e3-655a3bff9455" width="245px" alt="İyzico Ekranı" /> | <img src="https://github.com/user-attachments/assets/755bda5a-8f54-4b35-b270-d5f8623cd0cb" width="245px" alt=" Ödeme Başarılı Ekranı" /> |

### 👛 Cüzdan

| Bakiye Yükleme | Bakiye Görüntüleme|
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/0f27322d-2437-4f0a-9687-84b12faca2a6" width="245px" alt="Bakiye Yükleme Ekranı" /> | <img src="https://github.com/user-attachments/assets/ba8fb989-1933-4eba-b7c2-9094c3d37e21" width="245px" alt="Bakiye Görüntüleme" /> |

### 🕓 Kiralama Geçmişi & Profil

| Kiralama Geçmişi | Profil |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/4d486707-626f-48d9-aa57-454b076d60eb" width="245px" alt="Kiralama Geçmişi Ekranı" /> | <img src="https://github.com/user-attachments/assets/0b85fce4-d31c-4986-b322-3612d52c8f93" width="245px" alt="Profil" /> |

---

## 🤖 Yapay Zekâ Entegrasyonu (Google Gemini)

RenCar'ın harita ekranındaki **AI Öneri** özelliği, kullanıcının serbest metinle yazdığı isteği anlayıp mevcut araç listesinden en uygun olanları seçer.

**Nasıl çalışır?**

```
Kullanıcı sorgusu ──▶ AiRecommendationViewModel ──▶ AiRepository
                                                        │
                          araç listesi → toPromptLine() ┤ (data/mapper/AiMapper.kt)
                                                        ▼
                                              Gemini GenerativeModel
                                          (responseMimeType = application/json)
                                                        │
                                    JSON araç ID dizisi ┤ parseRecommendedIds()
                                                        ▼
                              MapIntent.SetAiRecommendations(ids) ──▶ MapViewModel
                                                        ▼
                                     MapUiState.recommendedVehicleIds → filtreli harita
```

- **`di/AiModule.kt`** — `GenerativeModel` Hilt ile tek noktadan sağlanır. Repository SDK'yı kendi gövdesinde kurmaz; böylece sahte bir model verilerek test edilebilir.
- **`data/repository/AiRepository.kt`** — Eşleştirme kurallarını içeren prompt'u kurar ve modeli çağırır.
- **`data/mapper/AiMapper.kt`** — Aracı prompt satırına çevirir, modelin JSON yanıtını ID listesine ayrıştırır.
- **`ui/map/AiRecommendationViewModel.kt`** — Diyaloğun kendi ViewModel'i; sonucu `onApply` ile `MapViewModel`'e bir intent olarak devreder (`MapIntent.SetAiRecommendations`). Böylece harita durumu tek elde kalır, filtre `ClearAiRecommendations` ile temizlenir.

Model, yanıtı **yalnızca JSON dizisi** olarak döndürmeye zorlanır (`responseMimeType = "application/json"`), böylece serbest metin ayrıştırma riski ortadan kalkar.

> ⚠️ **Gemini API anahtarı olmadan uygulama çalışır**, yalnızca AI öneri özelliği hata döndürür. Anahtarı almak için aşağıdaki [Gemini API Key Kurulumu](#-gemini-api-key-kurulumu) bölümüne bakın.

---

## 🔑 Gemini API Key Kurulumu

AI öneri özelliğini aktif etmek için ücretsiz bir Google Gemini API anahtarı gerekir.

### 1. Adım — API anahtarını alın

1. [**Google AI Studio**](https://aistudio.google.com/app/apikey) adresine gidin.
2. Google hesabınızla giriş yapın.
3. **"Create API key"** (API anahtarı oluştur) butonuna tıklayın.
4. Bir Google Cloud projesi seçin veya **"Create API key in new project"** ile yeni bir tane oluşturun.
5. Oluşan anahtarı kopyalayın — `AIza...` ile başlayan uzun bir metindir.

> 💡 Gemini API'nin ücretsiz katmanı (free tier) bu proje için fazlasıyla yeterlidir; kredi kartı bilgisi istemez.

### 2. Adım — Anahtarı projeye ekleyin

Projenin **kök dizinindeki** `local.properties` dosyasını açın (yoksa oluşturun) ve şu satırı ekleyin:

```properties
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

Dosyanın tamamı şuna benzemelidir:

```properties
sdk.dir=C\:\\Users\\<kullanıcı>\\AppData\\Local\\Android\\Sdk
GEMINI_API_KEY=AIzaSyXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
```

> 🔒 `local.properties` dosyası `.gitignore` içindedir — anahtarınız **asla** repoya gitmez.
> Anahtarı doğrudan `.kt` veya `.gradle.kts` dosyasına **yazmayın**.

### 3. Adım — Projeyi senkronize edin

Android Studio'da **Sync Project with Gradle Files** (🐘 ikonu) çalıştırın veya:

```bash
./gradlew clean assembleDebug
```

Anahtar, build sırasında `BuildConfig.GEMINI_API_KEY` alanına gömülür:

```kotlin
// app/build.gradle.kts
val geminiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
```

### 4. Adım — Test edin

Uygulamayı açın → **Harita** sekmesi → **AI Öneri** butonu → örnek sorgular:

| Sorgu | Beklenen sonuç |
|---|---|
| `en ucuz araç` | ECONOMY segmenti, fiyata göre artan sıralı |
| `aile için geniş araç` | Yüksek koltuklu / MINIVAN / STATION |
| `dağa gideceğim` | SUV kasa tipi |
| `2000 TL altı lüks otomatik` | COMFORT + otomatik vites + fiyat filtresi |

### ❓ Sorun Giderme

| Belirti | Çözüm |
|---|---|
| `API key not valid` | Anahtarı yeniden kopyalayın; başında/sonunda boşluk veya tırnak olmadığından emin olun |
| `404` / `model not found` | `di/AiModule.kt` içindeki `modelName` hesabınızın erişebildiği bir modelle eşleşmiyor. AI Studio'daki güncel model kimliğiyle değiştirin |
| Öneri hiç dönmüyor | `local.properties` **kök dizinde** mi? (`app/` altında değil) → Gradle sync yapın |
| `PERMISSION_DENIED` | Google Cloud projenizde **Generative Language API**'nin etkin olduğunu doğrulayın |
| `RESOURCE_EXHAUSTED` | Ücretsiz katman dakikalık limiti aşıldı; kısa süre bekleyin |
| Değişiklik etki etmiyor | `./gradlew clean` sonrası tekrar derleyin (BuildConfig yeniden üretilir) |

---

## 🛠️ Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| **Dil** | Kotlin 2.2.10 |
| **UI** | Jetpack Compose (BOM 2026.02.01) · Material 3 |
| **Mimari** | MVI (Model–View–Intent) |
| **State** | `StateFlow` + `collectAsStateWithLifecycle` |
| **DI** | Hilt (Dagger) · KSP |
| **Navigasyon** | Compose Navigation |
| **Ağ** | Retrofit · OkHttp · Kotlinx Serialization |
| **Gerçek Zamanlı** | Socket.IO Client |
| **Harita** | MapLibre GL Android SDK · Play Services Location |
| **Kamera & ML** | CameraX · ML Kit Face Detection |
| **Yapay Zekâ** | Google Generative AI (Gemini) |
| **Depolama** | AndroidX DataStore (Preferences) — token + tema tercihi |
| **Ödeme** | Iyzico · Cüzdan |
| **Build** | Gradle (Kotlin DSL) · Version Catalog |

---

## 🏛️ Mimari

Uygulama **MVI (Model–View–Intent)** desenini takip eder ve **tek yönlü veri akışı** ile öngörülebilir bir UI sağlar:

```
┌─ ui/ ─────────────────────────────────────────────────────────────────┐
│                                                                       │
│   Intent ──▶ ViewModel (reducer) ──▶ UiState ──▶ Screen               │
│      ▲            │                                │                  │
│      └────────────┼──────── kullanıcı aksiyonu ────┘                  │
│                   │   nav argümanları: SavedStateHandle               │
└───────────────────┼───────────────────────────────────────────────────┘
                    │
      Result<T>     │      Flow<VehiclePoint>            Flow<Boolean?>
 (exception yok)    │      (canlı konum)                 (token / tema)
┌─ data/ ───────────▼───────────────────────────────────────────────────┐
│                                                                       │
│   Repository ──▶ Api (Retrofit) ──▶ DTO                               │
│        ▲    │                        │                                │
│        │    │   └── Mapper (DTO → UI modeli) ┘                        │
│        │    │                                                         │
│        │    └──▶ socket/ RideLocationClient  (Socket.IO /ws/locations) │
│        │                                                              │
│        └──────── local/ TokenStore · ThemeStore  (DataStore)          │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```



Durumu olan her özellik `ui/<feature>/` paketi altında **üç çekirdek dosya** ile kurulur:

```
ui/<feature>/
├── <Feature>Contract.kt    # UiState (state) + Intent (kullanıcı aksiyonları)
├── <Feature>ViewModel.kt   # @HiltViewModel, tek giriş noktası: onIntent()
└── <Feature>Screen.kt      # Stateful sarmalayıcı + Stateless composable çifti
```

- **State** → Tek bir `data class <Feature>UiState`; tüm alanları varsayılan değerli, yalnızca saf UI durumunu tutar.
- **Intent** → `sealed interface`; parametreli aksiyonlar `data class`, parametresizler `data object`.
- **ViewModel** → `MutableStateFlow` içeride tutulur, dışarıya salt-okunur `StateFlow` olarak açılır; reducer mantığı `onIntent()` içindeki `when` bloğunda çalışır ve `copy()` ile yeni bir immutable state döner. Navigasyon argümanları (`vehicleId`, `rentalId` …) `SavedStateHandle` ile okunur.
- **Screen** → Stateful sarmalayıcı `collectAsStateWithLifecycle()` ile state'i toplar; stateless gövde yalnızca `uiState` + `onIntent` alarak UI çizer (preview & test dostu).

> **İstisna:** Kamera çekimi (`onCapture`) ve izin isteme (`onGrantPermission`) gibi Android launcher'larını tetikleyen callback'ler ek parametre olarak geçirilebilir — durumu değiştirmezler, sonuç yine bir intent ile ViewModel'e döner.

---

## 🔐 Oturum Yönetimi

Token akışı dört parçadan oluşur ve ekranlara sızmaz:

| Bileşen | Sorumluluk |
|---|---|
| `data/local/TokenStore.kt` | Access/refresh token'ların DataStore'da kalıcı saklanması (çıkışta yalnız token anahtarları silinir, tema tercihi korunur) |
| `data/remote/interceptor/AuthInterceptor.kt` | Giden her isteğe `Authorization` başlığının eklenmesi |
| `data/remote/interceptor/TokenAuthenticator.kt` | `401` alındığında yenilemenin tetiklenmesi ve isteğin tekrarı |
| `data/remote/session/SessionManager.kt` | `RefreshApi` ile tek seferlik (mutex korumalı) token yenileme; refresh de ölürse `forcedLogout` yayını |


Açılışta **Splash** ekranı saklı token ile oturumu geri yükler ve kullanıcıyı doğru ekrana yönlendirir: token yoksa Onboarding, ehliyet beklemedeyse LicensePending, **askıda rezervasyon/kiralama varsa ilgili kurtarma ekranı**, aksi hâlde Home.

---

## 🌗 Tema Yönetimi

Koyu/açık tema **uygulama içinden** değiştirilir; sistem ayarına gitmeye gerek yoktur.

```
ProfileScreen (gece/gündüz butonu) ──▶ ProfileIntent.ThemeToggled(dark)
        ──▶ ProfileViewModel ──▶ ThemeStore (DataStore)
                                      │
                MainViewModel.darkTheme ──▶ MainActivity ──▶ RenCarTheme(darkTheme = …)
```

- Tercih `Boolean?` olarak saklanır: `null` = kullanıcı henüz seçmedi → **sistem teması** takip edilir; `true`/`false` = açık seçim.
- `ThemeStore`, `TokenStore` ile **aynı** DataStore örneğini paylaşır (ayrı dosya açılmaz).
- Sistem teması sorgusu (`isSystemInDarkTheme()`) ViewModel'e sızmaz; `MainViewModel.darkTheme` ham tercihi taşır, `MainActivity` bunu `storedDarkTheme ?: isSystemInDarkTheme()` ile çözer.
- Tema oturumdan bağımsızdır — çıkış yapıldığında sıfırlanmaz, login ekranında da geçerlidir.

---

## 🛡️ Merkezî Hata Yönetimi (`util/`)

Hiçbir ViewModel hata mesajını elle (hard-coded string) üretmez. Ham `Throwable`'lar tek tip bir modele indirgenir ve kullanıcı metni **tek bir yerden** çözülür.

| Dosya | Sorumluluk |
|---|---|
| `util/AppError.kt` | Uygulama genelinde tek tip hata modeli (`sealed class AppError`) + `toAppError()` |
| `util/ErrorMessages.kt` | `AppError` → kullanıcıya gösterilebilir Türkçe metin (`toUserMessage()`) + ekran bağlamı (`ErrorContext`) |

**Ham hata → tek tip model**

```kotlin
sealed class AppError : Throwable() {
    data object Network : AppError()                 // IOException → bağlantı yok
    data class  Api(val code: Int) : AppError()      // HttpException → HTTP kodu korunur
    data class  Unknown(val original: Throwable? = null) : AppError()
}
```

```kotlin
// ViewModel içinde tek satırlık kullanım:
} catch (e: Exception) {
    _uiState.update {
        it.copy(
            isLoading = false,
            errorMessage = e.toAppError().toUserMessage(ErrorContext.RIDE_START),
        )
    }
}
```

Örneğin `409` kodu bağlama göre farklı metne çözülür:
- `ErrorContext.RESERVATION_CREATE` → *"Bu araç artık müsait değil veya zaten aktif bir rezervasyonunuz var."*
- `ErrorContext.RIDE_START` → *"Yolculuk başlatılamadı: fotoğraflar eksik veya yolculuk zaten başlamış."*
- `ErrorContext.PAYMENT_PAY` → *"Ödeme alınamadı: cüzdan bakiyesi yetersiz olabilir veya yolculuk zaten ödenmiş."*

Bağlam eşleşmezse koda göre genel bir yedek metne düşülür (*"Rezervasyon oluşturulamadı (409). Lütfen tekrar deneyin."*).

Mesaj tablosunun kaynağı [`docs/api/openapi.json`](docs/api/openapi.json) uç yanıtlarıdır; yeni bir uç eklendiğinde yalnızca `ErrorMessages.kt` güncellenir, ViewModel'lere dokunulmaz.

---

## 📁 Proje Yapısı

```
app/src/main/java/com/turkcell/rencar/
├── data/
│   ├── image/          # Fotoğraf sıkıştırma (ImageCompressor) + multipart parça üretimi (ImagePart)
│   ├── local/          # TokenStore · ThemeStore (aynı DataStore örneği)
│   ├── mapper/         # DTO ↔ UI model dönüşümleri + AiMapper (prompt/parse)
│   ├── model/          # UI modelleri (VehicleUi, RentalUi, ReservationUi, QuoteUi, ...)
│   ├── remote/
│   │   ├── api/            # Auth · Refresh · Vehicle · Rental · Reservation · License · Card · Wallet · Iyzico
│   │   ├── dto/            # İstek/yanıt DTO'ları
│   │   ├── interceptor/    # AuthInterceptor · TokenAuthenticator
│   │   ├── session/        # SessionManager (token yenileme + forcedLogout)
│   │   └── socket/         # RideLocationClient (Socket.IO canlı konum, /ws/locations)
│   └── repository/     # Ai · Auth · License · Payment · Rental · Reservation · Vehicle · Wallet
├── di/                 # Hilt modülleri (Network · Ai · DataStore · Coroutine)
├── ui/                 # Özellik ekranları (MVI)
│   ├── splash/ · onboarding/ · login/ · register/ · otp/
│   ├── license/ · licensepending/ · selfie/
│   ├── home/ · map/ · vehicledetail/ · reservation/
│   ├── rentalphotos/ · activerental/ · rentalreturnphotos/
│   ├── payment/ · wallet/ · rentals/ · profile/
│   ├── components/ · navigation/ · theme/ · icons/
│   └── MainViewModel.kt    # Uygulama seviyesi: forcedLogout + tema tercihi
├── util/               # AppError · ErrorMessages
├── RenCarApplication.kt
└── MainActivity.kt
```

---

## 🚀 Kurulum

### Gereksinimler

- **Android Studio** (Ladybug veya üzeri önerilir)
- **JDK 11** (kaynak/hedef uyumluluğu; derlemede Android Studio'nun paketlediği JBR kullanılması önerilir)
- **Android SDK** — compileSdk 36.1, minSdk 24, targetSdk 36
- **Google Gemini API Key** (AI önerisi için — bkz. [Gemini API Key Kurulumu](#-gemini-api-key-kurulumu))

### Adımlar

**1.** Depoyu klonlayın:

```bash
git clone https://github.com/mustafaderinoz/Rencar.git
cd Rencar
```

**2.** Kök dizinde `local.properties` dosyasını oluşturun/düzenleyin:

```properties
sdk.dir=/path/to/Android/Sdk
GEMINI_API_KEY=buraya_api_anahtarinizi_yapistirin
```

**3.** Projeyi Android Studio'da açın ve Gradle senkronizasyonunun tamamlanmasını bekleyin.

**4.** Uygulamayı çalıştırın:

```bash
./gradlew installDebug
```

veya Android Studio üzerinden **Run** ▶️

> ℹ️ Backend adresi `app/build.gradle.kts` içinde `BuildConfig.BASE_URL` olarak gömülüdür. Kendi sunucunuza bağlanmak için bu alanı değiştirin.

### 📱 İzinler

Uygulama çalışma zamanında şu izinleri ister:

| İzin | Kullanım amacı |
|---|---|
| `CAMERA` | Ehliyet çekimi, selfie doğrulama, araç durum fotoğrafları |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Haritada anlık konum ve yakındaki araçlar |
| `INTERNET` | API ve Socket.IO bağlantısı |

> 💡 Emülatörde test ederken konumu **Extended Controls → Location** üzerinden ayarlayın; aksi hâlde harita araçları uzakta görünebilir.

---

## 📚 Detaylı Dokümantasyon

Mimari kararlar ve tasarım notları için:

- [`decisions.md`](decisions.md) — Teknik-mimari kararlar ve gerekçe geçmişi
- [`docs/archıtecture/`](docs/archıtecture/) — [MVI genel bakış](docs/archıtecture/MVI-overview.md) · [kontratlar](docs/archıtecture/MVI-contracts.md) · [ViewModel kuralları](docs/archıtecture/MVI-viewmodel-rules.md)
- [`docs/design/`](docs/design/) — [Renk sistemi](docs/design/color-system.md) · [Tipografi sistemi](docs/design/01-typography-system.md)
- [`docs/api/openapi.json`](docs/api/openapi.json) — Backend API sözleşmesi

---

## 📝 Geliştirme Kuralları

Bu projede çalışan herkesin (insan/AI) uyması gereken kurallar [`AGENTS.MD`](AGENTS.MD) dosyasında tanımlıdır. Özetle:

- 🔢 Tek seferde en fazla **5 ilişkili dosya** üzerinde çalışılır
- 🚫 **Uydurmak yasak** — eksik bilgi varsa operasyon durdurulur ve sorulur
- 📋 **Önce planla, sonra kodla** — dosya dökümü ve bağımlılık matrisi olmadan implementasyona başlanmaz
- 🧩 **MVI referans kalıbı** (`ui/login/*`) birebir izlenir
- 🗒️ Her mimari karar gerekçesiyle `decisions.md`'ye yazılır

---

## 👥 Geliştiriciler

* **Erdem Akatay** — [GitHub](https://github.com/erdemakatay)
* **Edanur Çıtak** — [GitHub](https://github.com/edanurcitak)
* **Mustafa Derinöz** — [GitHub](https://github.com/mustafaderinoz)

