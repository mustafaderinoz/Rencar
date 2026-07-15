Projede verilen bütün mimarisel-teknik kararları ve karar geçmişini içeren dökümantasyondur.

---

### Minimum Değişiklik İlkesi (Mimari Yön · üst-düzey kabul kriteri)

- Seçim: **Mimari kararlar, backend/kütüphane değişikliklerinde kodun MİNİMUM değişmesini sağlayacak şekilde alınır.** Dış değişimin etkisi tek bir sınıra hapsedilir.

- Son Güncelleme Tarihi: 14.07.2026

- Sebep: Dış değişimlerin (API şeması, base URL, kütüphane sürümü) yayılma yüzeyi daraltılmalı. Somut uygulamalar:
  - **Base URL / endpoint** → tek `BuildConfig.BASE_URL` + `di/NetworkModule` (URL değişimi = 1 satır).
  - **API şeması** → DTO'lar `data/remote/dto`'da izole; UI'a DTO doğrudan verilmez. DTO → model dönüşümü **ayrı bir mapper katmanında** yapılır (repository'nin içinde değil). Additive alanlar nullable-default ile, breaking alanlar mapper katmanında emilir.
  - **Kütüphane** → kullanım repository/di ardında; UI kütüphaneye doğrudan bağımlı kalmaz.

- Not: Bu ilke tüm mimari kararların üstünde bir kabul kriteridir; bir karar bu ilkeyi ihlal ediyorsa gerekçelendirilmelidir.

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

- Seçim: **data + repository + ayrı mapper katmanı** (ViewModel → Repository → ApiService; **DTO → model dönüşümü repository'nin DIŞINDA, kendi mapper katmanında** yapılır).

- Son Güncelleme Tarihi: 14.07.2026 (ilk: 09.07.2026)

- Alternatifler: **domain + UseCase tier yığını** (tam Clean Architecture)

- Sebep: DTO'lar (API şeması) UI'a doğrudan sızmamalı; DTO → UI/domain modeli dönüşümü **ayrı bir mapper katmanında** (kendi paketi/sınıfı) yapılır (anti-corruption sınırı). Mapping mantığı repository'nin İÇİNE yazılmaz; repository yalnızca mapper'ı çağırır. Böylece backend şema değişikliği tek noktada (mapper katmanı) emilir; repository/ViewModel/UI etkilenmez ("Minimum Değişiklik İlkesi").

- **Kural (14.07.2026 güncellemesi):** DTO→model mapping'i **ayrı bir katmanda** (kendi mapper paketi) yapılır; bu var olan mapper/model katmanı serbestçe değiştirilebilir. Ancak bunun ötesinde **+1 yeni mimari katman** (ör. UseCase tier'ı) EKLENMEZ — izolasyon mapper katmanıyla sınırlıdır. Bu kural, önceki *"ayrı domain/mapping katmanı eklenmez"* kararının yerini alır. (AGENTS §4.6 "usecase/domain **katmanı** varsayılan olarak eklenmez" ile uyum: eklenen şey tam bir domain/UseCase tier'ı değil, ince bir mapper/model katmanıdır.)


### Vehicle Akışı — DTO İzolasyonu

- Seçim: **`VehicleResponse` (DTO) UI'a doğrudan verilmez; ayrı bir mapper katmanı (`VehicleMapper`) `VehicleResponse` → `VehicleUi` (model) çevirir.** Repository yalnızca mapper'ı çağırır; Harita + Araç Detay ekranları `VehicleUi` kullanır.

- Son Güncelleme Tarihi: 14.07.2026

- Alternatif (eski durum): DTO'yu doğrudan UiState/composable'a taşımak — **mevcut kod böyle** (`VehicleResponse` 5 UI dosyasında geçiyor: `MapContract`, `VehicleDetailContract`, `VehicleDetailViewModel`, `VehicleDetailScreen`, `RencarMap`).

- Sebep: "Minimum Değişiklik İlkesi" + "Katman Derinliği" kuralı gereği; breaking API şema değişimi tek noktada (mapper katmanı) emilsin, UI/ViewModel etkilenmesin.

- **Not (14.07.2026): Karar alındı ve KOD HİZALANDI.** Ayrı mapper/model katmanı eklendi (`data/model/*Ui` + `data/mapper/*Mapper`). Tüm akışlar repository üzerinden model döndürür: Vehicle (`VehicleUi`), Reservation/Quote (`QuoteUi`), Auth/Profil/License (`UserUi` + `LicenseVerificationStatus`), Rental (`RentalUi` + `RentalPhotosUi`). Kullanılmayan yanıtlar `Result<Unit>`'e indirgendi (login/reserve/upload/startRental). `ui/` katmanında hiçbir `data.remote.dto` import'u kalmadı (grep ile doğrulandı); `:app:compileDebugKotlin` başarılı.


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


### Mahalle Adı & "~dk" Uzaklık Etiketi (Alt Kart Altyazısı)

- Seçim: **Android cihaz `Geocoder`** (reverse-geocode) + mesafeden yürüme süresi tahmini (~80 m/dk)

- Son Güncelleme Tarihi: 14.07.2026

- Alternatifler: **Sadece mesafe (m/km)** göster, **altyazıyı kaldır**

- Sebep: Tasarımdaki "Kadıköy çevresinde · 3 dk uzaklıkta" satırının RenCar API'sinde KARŞILIĞI YOK (geocoding/semt ucu yoktur — §2.2). Mahalle adı cihazın kendi Geocoder'ıyla (IO dispatcher, bloklayıcı API) çözülür; en yakın araca ~dk mesafeden tahmin edilir. Geocoder yoksa/başarısızsa altyazı sessizce gizlenir. Üstteki arama çubuğu da aynı nedenle DEKORATİFtir (arama ucu yok).