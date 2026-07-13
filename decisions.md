Projede verilen bütün mimarisel-teknik kararları ve karar geçmişini içeren dökümantasyondur.

---

### Dependency Injection Kütüphanesi

- Seçim*: **Hilt**

- Son Güncelleme Tarihi*: 02.07.2026

- Alternatifler: **Koin**

- Sebep: **Opsiyonel**


### Navigasyon

- Seçim: **Compose Navigation**

- Son Güncelleme Tarihi: 02.07.2026


### Sunum Katmanı Mimari Deseni

- Seçim: **MVI (Model–View–Intent)**

- Son Güncelleme Tarihi: 03.07.2026

- Alternatifler: **MVVM**

- Sebep: Tek yönlü veri akışı + tek kaynak (single source of truth) state; test edilebilir ve öngörülebilir UI.


### State Yönetimi (Sunum)

- Seçim: **Kotlin StateFlow + collectAsStateWithLifecycle**

- Son Güncelleme Tarihi: 03.07.2026

- Alternatifler: **LiveData**

- Sebep: Compose ile lifecycle-aware, sıcak (hot) state yayını.


### ViewModel Enjeksiyonu

- Seçim: **Hilt + hiltViewModel()** (androidx.hilt:hilt-navigation-compose)

- Son Güncelleme Tarihi: 04.07.2026

- Alternatifler: **Manuel ViewModelProvider.Factory**

- Sebep: Mevcut DI kararı (Hilt) ile tutarlılık.

- Not: androidx.hilt **1.3.0** ile `hiltViewModel()` deprecated-olmayan hâliyle `androidx.hilt.lifecycle.viewmodel.compose` paketinden import edilir; `androidx.hilt.navigation.compose` içindeki kopya kullanımdan kaldırılmıştır. Bağımlılık olarak `hilt-navigation-compose` tutulur (Compose Navigation ile navigasyon-scope'lu VM için) ve bu paket lifecycle-viewmodel-compose'a transitively bağımlıdır.


### Ağ Katmanı (HTTP İstemcisi) ve JSON Serileştirme

- Seçim: **Retrofit + kotlinx.serialization** (OkHttp motoru; `retrofit2-kotlinx-serialization-converter` köprüsü)

- Son Güncelleme Tarihi: 09.07.2026

- Alternatifler: **Retrofit + Moshi**, **Ktor Client**

- Sebep: Kotlin-native serializer (KSP gerektirmez), Hilt/coroutines ile temiz uyum. OkHttp `logging-interceptor` (debug) + auth interceptor kullanılır.


### Token Saklama

- Seçim: **DataStore Preferences** (`androidx.datastore:datastore-preferences`)

- Son Güncelleme Tarihi: 09.07.2026

- Alternatifler: **EncryptedSharedPreferences** (Jetpack Security — kısmen deprecated)

- Sebep: Coroutine/Flow tabanlı asenkron erişim; access + refresh token burada tutulur, auth interceptor buradan okur.


### Katman Derinliği (Data)

- Seçim: **data + repository** (ViewModel → Repository → ApiService)

- Son Güncelleme Tarihi: 09.07.2026

- Alternatifler: **domain katmanı + UseCase** (Clean Architecture)

- Sebep: AGENTS §4.6 minimal soyutlama varsayılanıyla uyumlu; ayrı domain/UseCase katmanı eklenmez.


### API Base URL

- Seçim: **`https://rencarv2.halitkalayci.com/`** (`BuildConfig.BASE_URL`)

- Son Güncelleme Tarihi: 13.07.2026

- Sebep: Endpoint'ler KÖK dizinde yayınlanıyor; `/api` prefix'i YOKTUR. Yalnızca Swagger UI `/api/docs` altındadır. curl ile doğrulandı: `/health` → 200.

- Not (13.07.2026): **v1 → v2 geçişi.** Eski `rencar.halitkalayci.com` sunucusu `docs/api/openapi.json`'ın gerisindeydi (araç yeni alanlarını — pricePerMinute/fuelPercent/seats vb. — döndürmüyordu, bu yüzden detay ekranı `—` gösteriyordu). `rencarv2.halitkalayci.com` openapi.json ile birebir paralel (path/şema/alan farkı yok; `/reservations`, `/wallet` dahil). curl `swagger-ui-init.js` probe ile doğrulandı.


### Kamera & Yüz Algılama (Ehliyet + Selfie doğrulama)

- Seçim: **CameraX (1.4.2) + ML Kit Face Detection (16.1.7, bundled)**

- Son Güncelleme Tarihi: 09.07.2026

- Alternatifler: **Camera2 API (düşük seviye)**, **ML Kit unbundled (Play Services)**

- Sebep: Selfie ekranında canlı önizleme + gerçek zamanlı yüz ortalama; CameraX lifecycle-bağlı ve Compose (`PreviewView` + `AndroidView`) ile temiz. ML Kit bundled model offline çalışır (Play Services'e bağımlı değil). Selfie yalnızca client-side liveness kapısıdır; backend'e GÖNDERİLMEZ (`UploadLicenseDto` yalnız front+back alır).


### Ehliyet Görsel Kaynağı

- Seçim: **Harici kamera çekimi** (`ActivityResultContracts.TakePicture` + `FileProvider`)

- Son Güncelleme Tarihi: 09.07.2026

- Alternatifler: **Galeriden seçim (PhotoPicker)**, **CameraX ile uygulama içi çekim**

- Sebep: Ehliyet ön/arka yüz belge fotoğrafı; harici kamera basit ve yeni bağımlılık gerektirmez. Çekilen dosyalar `filesDir/licenses/` altında tutulur, FileProvider ile paylaşılır. Galeri yükleme bu iş kapsamında değil.


### Görsel Yükleme & Sıkıştırma (License Upload)

- Seçim: **Multipart (Retrofit `@Multipart`) + yüklemeden önce JPEG downscale/compress** (`data/util/ImageCompressor`)

- Son Güncelleme Tarihi: 09.07.2026

- Sebep: `POST /license/upload` dosya başına maks. 5MB; kamera tam çözünürlük bunu aşabilir. Yüklemeden önce ~1600px'e küçültülüp q80 sıkıştırılır. Auth token'ı mevcut `AuthInterceptor` üzerinden eklenir.