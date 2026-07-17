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


### Oturum Yönetimi & Token Yenileme (SessionManager)

- Seçim: **`SessionManager` + OkHttp `Authenticator` (TokenAuthenticator) ile otomatik token yenileme.** Herhangi bir Retrofit çağrısı **401** dönünce access token, eldeki refresh token'la (`POST /auth/refresh`, rotation) sessizce yenilenir ve istek taze token'la BİR kez tekrar denenir. Yenileme **tek-uçuşludur** (`Mutex` — aynı anda gelen çok sayıda 401 için tek ağ çağrısı). Refresh de başarısızsa token'lar temizlenir ve `SessionManager.forcedLogout` olayı yayınlanır; `RencarNavHost` bunu dinleyip kullanıcıyı Login'e atar (backstack temizlenir).

- Son Güncelleme Tarihi: 15.07.2026

- Alternatifler: **Her istekte 401'i ViewModel'de ele alıp elle login'e yönlendirme** (kod tekrarı; her ekran ayrı ayrı ilgilenir), **response interceptor içinde yenileme** (OkHttp sözleşmesi 401 için `Authenticator`'dır; tekrar-deneme ve `priorResponse` döngü koruması hazır gelir).

- Sebep ("Minimum Değişiklik İlkesi"): Token yenileme tek bir sınıra (`Authenticator` + `SessionManager`) hapsedildi; ViewModel/UI'daki mevcut 401 mesajları ve akışlar **değişmedi** (Faz 1 sıfır UI dokunuşu). Backend refresh ucu zaten mevcut (§2.2'ye uygun; uydurma yok).

- **Dairesel bağımlılık çözümü:** `SessionManager`'ın kullandığı `RefreshApi`, DI'da ana istemciden AYRI ve **`AuthInterceptor`/`Authenticator` İÇERMEYEN** sade bir OkHttp/Retrofit'ten üretilir (`di/NetworkModule.provideRefreshApi`). Böylece (a) `Authenticator → SessionManager → RefreshApi` grafiği asiklik olur, (b) refresh çağrısı 401 dönse bile Authenticator'a uğramaz, kendini tetikleyemez. Sonsuz döngü ayrıca `priorResponse` sayımıyla (en çok 1 tekrar) sınırlanır.

- **Dokunulan/eklenen dosyalar:** yeni `data/remote/api/RefreshApi`, `data/remote/session/SessionManager`, `data/remote/interceptor/TokenAuthenticator`, `ui/MainViewModel`; güncellenen `data/remote/dto/AuthDtos` (RefreshTokenRequest), `data/local/TokenStore` (currentRefreshToken), `di/NetworkModule`, `ui/navigation/RencarNavHost`.

- Not (15.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.** `RideLocationClient`'in socket `connect_error → refresh → reconnect` dalı da geri bağlandı (aşağıdaki "Aktif Yolculuk" sapma notuna bakınız).


### Açılışta Oturum Geri Yükleme (Session Restore / Splash)

- Seçim: **Yeni `SPLASH` başlangıç ekranı** (MVI §4 kalıbı: `ui/splash/{SplashContract,SplashViewModel,SplashScreen}`). Uygulama açılışında saklı access token varsa `GET /auth/me` çağrılır; **süresi dolmuş access token, `TokenAuthenticator` üzerinden refresh token'la sessizce yenilenir** (yukarıdaki "Oturum Yönetimi & Token Yenileme" altyapısını doğrudan kullanır). Sonuca göre yönlendirilir: token yok → Onboarding; `me()` başarılı → rol/ehliyet durumuna göre Home/License/LicensePending; **401** (refresh de öldü, oturum temizlendi) → Login; ağ hatası → "Tekrar Dene". `startDestination` `ONBOARDING`'den `SPLASH`'e çevrildi.

- Son Güncelleme Tarihi: 17.07.2026

- Alternatifler: **`startDestination`'ı token'a göre dinamik seçme** (auth kontrolü ilk kompozisyondan önce bitmeli → yükleme kapısı yine gerekir), **her ekranda ayrı auth-guard** (kod tekrarı). Ayrı bir splash hedefi + "çöz → popUpTo(SPLASH, inclusive) ile geç" kalıbı seçildi; bu, OTP sonrası "çöz → popUpTo(ONBOARDING) ile geç" kalıbıyla tutarlı.

- Sebep: Önceden `startDestination = ONBOARDING` sabitti; girişli kullanıcı bile her açılışta Onboarding→Login→OTP'ye zorlanıyor, saklı refresh token açılışta **hiç kullanılmıyordu**. Splash, mevcut `SessionManager`/`Authenticator` motorunu bir `me()` çağrısıyla tetikleyerek oturumu geri yükler (yeni bağımlılık/uç YOK — §2.2).

- **Yönlendirme kuralı tekrarı (bilinçli):** `SplashViewModel.resolveDestination`, `OtpVerificationViewModel.resolveDestination` ile AYNI kuralı taşır (PENDING + `GET /license/status` → LICENSE_PENDING/HOME/LICENSE_UPLOAD). Ortak bir UseCase katmanı EKLENMEDİ (§4.6 / "Katman Derinliği" — +1 tier yok); kodun çalışan OTP akışına dokunmamak için ("Minimum Değişiklik") kural iki VM'de replike edildi; ikisi birlikte güncel tutulmalıdır (kod içinde çapraz-referans notu var).

- **`forcedLogout` navigasyonu güncellendi:** başlangıç artık SPLASH olduğundan ve oto-login'le doğrudan Home'a girildiğinde geri yığında ONBOARDING bulunmadığından, sert-logout `popUpTo(RencarDestinations.ONBOARDING)` yerine `popUpTo(navController.graph.id) { inclusive = true }` ile tüm grafiği temizler. Diğer `popUpTo(ONBOARDING)` kullanımları yalnız token'sız akışta (Onboarding yığında) çalıştığından değişmedi.

- **Dokunulan/eklenen dosyalar:** yeni `ui/splash/{SplashContract,SplashViewModel,SplashScreen}`; güncellenen `ui/navigation/RencarDestinations` (SPLASH), `ui/navigation/RencarNavHost` (startDestination + splash composable + forcedLogout popUpTo).

- Not (17.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.**


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


### Araç Listeleme — Sayfalama (GET /vehicles)

- Seçim: **`VehicleRepository.getAvailableVehicles` TÜM sayfaları dolaşır** (`limit=100` — API maks., `page` 1'den artarak; eksik dolu sayfa gelince durur) ve birleştirir. Önceden tek çağrı yapılıyor, `page`/`limit` gönderilmiyordu.

- Son Güncelleme Tarihi: 17.07.2026

- Sebep (BUG düzeltmesi): `GET /vehicles` **sayfalıdır**; `limit` gönderilmezse sunucu yalnızca varsayılan ilk sayfayı (**20 araç**, en son eklenenler) döner. Seed verisinde en yeni araçlar Ankara/Eskişehir olduğundan, İstanbul'daki ~118 araç sonraki sayfalarda kalıyor ve haritada **hiç görünmüyordu** ("araçlar listelenmiyor" şikâyeti). Doğrulandı: `limit=100` → 100 araç (69 İstanbul), `page=2` → 49 araç (hepsi İstanbul). API'de konum/şehir filtresi YOK (§2.2), bu yüzden istemci tüm araçları çekip haritada görünürdekileri eler.

- Alternatifler: **Tek sayfa `limit=100`** (max 100 < toplam ~149 → yine eksik kalır), **bbox/konum filtresi** (API'de yok — §2.2 uydurma yasak).

- Not: Değişiklik tek dosyada (`data/repository/VehicleRepository`), mimariye uygun (data katmanı; ViewModel/UI/mapper etkilenmez). `MAX_PAGES=20` güvenlik sınırı sonsuz döngüyü engeller. **Gerçek cihazda doğrulandı** — İstanbul konumunda harita 146 araçla doldu (öncesinde 20). `:app:assembleDebug` başarılı.


### Harita — Araç Kümeleme (Clustering)

- Seçim: **MapLibre yerel kümelemesi** (`GeoJsonOptions.withCluster/withClusterRadius/withClusterMaxZoom`). `vehicles` kaynağı kümelenir; tekil araç `SymbolLayer`'ına `!has("point_count")`, kümeler için ayrı `clusters-layer`'a `has("point_count")` filtresi konur. `clusterRadius` PİKSEL olduğundan zoom arttıkça kapsanan coğrafi alan küçülür → kümeler **zooma bağlı** ayrışır; `clusterMaxZoom` üstünde araçlar tek tek görünür. Kümeye dokununca yakınlaşılır (küme açılır).

- Son Güncelleme Tarihi: 17.07.2026

- Alternatifler: **İstemci tarafı ızgara kümeleme** (kamera her hareketinde elle yeniden hesap — motorun yaptığını tekrarlar), **plugin-annotation `SymbolManager` + ClusterOptions** (ikinci bir işaretçi sistemi; mevcut GeoJSON/SymbolLayer akışına ekstra katman).

- Sebep: 146 araç düşük zoom'da üst üste binip haritayı okunmaz yapıyordu. Yerel kümeleme zoom'a tepkiyi motor tarafında (yeniden kümeleme) otomatik verir; yalnızca render'ı biz yaparız.

- **Glyph kısıtı (mevcut karar korunur):** OSM raster stili font/glyphs içermez → küme sayısı native `text-field` ile YAZILAMAZ. Fiyat balonlarındaki gibi sayı Canvas ile bitmap'e çizilir: `iconImage` ifadesi `"cluster-<sayı>"` ikon adını üretir, bitmap **talep anında** `MapView.addOnStyleImageMissingListener` ile üretilip stile eklenir ve önbelleklenir (zoom/pan ile sayı değiştikçe yeni ikon istenir). Glyph/font sunucusu (harici bağımlılık) EKLENMEDİ.

- **Dokunulan dosyalar:** `ui/map/RencarMap` (kümeli kaynak + filtreli tekil katman + clusters-layer + OnStyleImageMissing + kümeye-dokun-yakınlaş), `ui/map/VehicleMarkers` (`buildCluster` sayı balonu bitmap'i). **Yeni bağımlılık YOK.**

- Not (17.07.2026): **Gerçek cihazda doğrulandı** — z10'da büyük kümeler (17/13/9…), yakınlaştıkça küçük kümelere (7/5/2) ve tekil fiyat balonlarına ayrışıyor. `:app:assembleDebug` başarılı.


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


### Aktif Yolculuk — Canlı Konum (Socket.IO) + Anlık Ücret (Poll)

- Seçim: **Araç canlı konumu `Socket.IO` (`io.socket:socket.io-client:2.1.0`) ile `/ws/locations` namespace'inden `my-vehicle` event'iyle alınır; anlık ücret/mesafe/geçen süre `GET /rentals/active` (`ActiveRentalResponseDto`) periyodik poll'üyle (~4 sn) beslenir.** Geçen süre ekranda 1 sn'lik yerel sayaçla akıp her poll'de sunucudaki `elapsedSeconds` ile senkronlanır.

- Son Güncelleme Tarihi: 15.07.2026

- Alternatifler: **Sadece poll (harita canlılığı düşük)**, **WebSocket'i doğrudan OkHttp ile (sözleşme Socket.IO)**

- Sebep: "Anlık ücret API ile hesaplanmalı" → sunucudaki `currentCost` tek doğruluk kaynağıdır (istemci fiyat hesaplamaz). Harita canlılığı için socket sözleşmesi sunucu tarafından `/ws/locations`+`my-vehicle` olarak verildi. Kütüphane kullanımı **repository/di ardında** (`data/remote/socket/RideLocationClient` → `RentalRepository.vehiclePositionStream()`); UI yalnız `Flow<VehiclePoint>` görür, `io.socket`'e bağımlı kalmaz (decisions.md "Minimum Değişiklik" + "Kütüphane"). DTO izolasyonu korunur: `ActiveRentalResponse`/`FinishRentalResponse` → `ActiveRentalUi`/`RentalReceiptUi` mapper katmanında çevrilir.

- **Örnek koddan sapmalar (§2.2 uydurmak yasak):**
  - `RideLocationClient` — **GÜNCELLENDİ (15.07.2026):** Artık `SessionManager` var; örnekteki `connect_error → refreshSession() → reconnect` dalı geri kondu. Token socket kurulurken okunduğundan, handshake reddinde taze token alınıp socket yeniden kurulur; deneme sayısı `MAX_AUTH_RETRIES` ile sınırlı, başarılı bağlantıda (`EVENT_CONNECT`) sıfırlanır. `TokenStore.accessToken()` yok → suspend `currentAccessToken()` kullanıldı. `BASE_URL` sonda `/` içerdiğinden namespace eklenmeden `trimEnd('/')` ile kırpıldı. (Oturum yönetimi kararı için bkz. "Oturum Yönetimi & Token Yenileme (SessionManager)".)
  - **"Kilitle / Aç" butonu** — openapi.json'da kilit/aç (lock/unlock) ucu YOK. Buton **yalnız yerel görsel toggle**tır (ağ çağrısı yapmaz); gerçek uç eklenince bağlanır. Kullanıcı onayıyla bu davranış seçildi.
  - **Ödeme** — ~~"Kiralamayı Bitir" sonrası ekran ücret dökümüyle KALIR; `POST /rentals/{id}/pay` bu iş kapsamında değildir~~ **(16.07.2026 GÜNCELLENDİ — artık ödeme ekranı var; bkz. "Ödeme Ekranı (Cüzdan/Kart + Kart Ekle)" kararı).** Finish başarılı olunca `active_rental` backstack'ten çıkıp `payment/{rentalId}` ekranına otomatik geçilir.

- **Harita componentleştirme:** `ui/map/RencarMap` zaten bağımsız composable'dı; home'a bağlı değildi. Opsiyonel `ridePoint: LatLng?` parametresi eklendi (tekil araç pin'i + kamera takibi); ridePoint null iken home davranışı değişmez. Böylece aynı harita hem Home hem Aktif Yolculuk ekranında kullanılır. Pin bitmap'i `VehicleMarkers.buildRidePin` (mavi daire + beyaz araç silüeti).

- Not (15.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.** Yeni ekran `ui/activerental/*` (MVI §4 kalıbı: Contract+ViewModel+Screen). Akış: RentalPhotos "Kiralamayı Başlat" (`POST /rentals/{id}/start`) → `active_rental/{rentalId}` (foto ekranı backstack'ten çıkar). Tarih ayrıştırma minSdk 24 + desugaring kapalı olduğundan `SimpleDateFormat` ('X' ISO ofseti) ile yapıldı (java.time API 26 ister).


### Ödeme Ekranı (Cüzdan/Kart + Kart Ekle) — `POST /rentals/{id}/pay`

- Seçim: **Kiralama bitince (`POST /rentals/{id}/finish`) otomatik olarak yeni `payment/{rentalId}` ekranına geçilir; kullanıcı cüzdan veya kayıtlı kartla öder (`POST /rentals/{id}/pay`), opsiyonel indirim kodu girer, başarıda Home'a döner.** Kart kaydetme AYRI SAYFA DEĞİL, ekran içi **pop-up (`androidx.compose.ui.window.Dialog`)**: marka (VISA/Mastercard) + son 4 hane + SKT ay/yıl → `POST /cards`.

- Son Güncelleme Tarihi: 16.07.2026

- Alternatifler: **Butonla geçiş** (finish sonrası ActiveRental'da özet kalıp "Ödemeye Geç" butonu) — kullanıcı otomatik geçişi seçti. **Ödeme sonrası ekranda kalma** — kullanıcı Home'a dönüşü seçti.

- Sebep: Tasarımdaki "Yolculuk tamamlandı" başlıklı ödeme ekranı, finish'in doğal devamıdır. Ekran verisini nav argümanıyla taşımak yerine **`GET /rentals/{id}` ile döküm yeniden çekilir** (süreç ölümüne dayanıklı; tek doğruluk kaynağı sunucu). "Kiralama ücreti" kalemi türetilir: `usageFee = totalPrice − startFee − serviceFee`. Kart yalnız görsel meta (marka+son4+SKT) olarak tutulur — tam kart numarası/CVV backend'de de reddedilir (PCI kapsamı dışı, §2.2 uydurma yok).

- **DTO izolasyonu korunur (decisions.md "Katman Derinliği"):** `PaymentDtos` (CardResponse/CreateCardRequest/WalletResponse/PayRentalRequest/PayRentalResponse) → `PaymentModels` (CardUi/PaymentReceiptUi/PaymentResultUi) `PaymentMapper` ile çevrilir; `ui/payment/*` katmanında `data.remote.dto` importu yoktur. `RentalResponse` DTO'suna döküm alanları (totalPrice/serviceFee/durationMinutes/distanceKm/paymentStatus) **additive + nullable-default** eklendi ("Minimum Değişiklik").

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli.

- **Kararlar/sapmalar:**
  - İndirim kodu ödeme anında sunucuda uygulanır (`discountCode`); makbuz `paidAmount`/`discountAmount` döner. `explicitNulls=false` sayesinde WALLET'ta `cardId=null` gövdeye yazılmaz (openapi: WALLET'ta cardId verilirse 400).
  - **İndirim kodu — önizleme YOK (§2.2):** openapi.json'da müşteriye açık indirim-kodu doğrulama/önizleme ucu yoktur (yalnız `POST /rentals/{id}/pay` uygular + admin CRUD). Bu yüzden kod ödemeden ÖNCE tutardan düşülemez; ekranda "kod ödeme sırasında uygulanacak" ipucu gösterilir, geçersiz kod ancak ödeme anında anlaşılır ve hata mesajı **kod-özel** hale getirilir (404/409'da `hasDiscount` bayrağıyla ayrıştırma). Makbuzda indirim + ödenen tutar net gösterilir.
  - **Kart aksiyonları (16.07.2026):** kart satırında "Varsayılan yap" (`PATCH /cards/{id}/default`) ve **"Sil" metni** (çöp kutusu ikonu yerine; `DELETE /cards/{id}`, önce onay pop-up'ı) var. Silinen kart seçiliyse seçim öntanımlıya/ilk karta düşürülür.
  - **Tasarım (16.07.2026):** ekran yeniden düzenlendi — yolculuk özeti + Süre/Mesafe tek kartta, ücret dökümü ayrı kart, segmentli (pill) Cüzdan/Kart seçici, seçilebilir kart görünümleri, sabit alt "Ödenecek tutar + Öde" çubuğu + güvenlik ipucu, makbuz stili "Ödendi" ekranı.
  - Cüzdan bakiyesi < toplam ise "Öde" pasif + uyarı (client tarafı kapı; sunucu da 409 döner).
  - Açılışta döküm/kart/bakiye **paralel** yüklenir; döküm kritiktir (başarısızsa tam ekran hata), kart/bakiye hatası sessizce boş/0 sayılır.
  - Navigasyon ekran katmanında (ActiveRentalScreen `LaunchedEffect(isFinished)` → `onNavigateToPayment`); VM'de Effect kanalı eklenmedi (§4.6).

- **Dokunulan/eklenen dosyalar:** yeni `data/remote/dto/PaymentDtos`, `data/remote/api/CardApi`, `data/remote/api/WalletApi`, `data/model/PaymentModels`, `data/mapper/PaymentMapper`, `data/repository/PaymentRepository`, `ui/payment/{PaymentContract,PaymentViewModel,PaymentScreen}`; güncellenen `data/remote/dto/RentalDtos` (RentalResponse döküm alanları), `data/remote/api/RentalApi` (getRental+pay), `di/NetworkModule` (CardApi/WalletApi), `ui/navigation/{RencarDestinations,RencarNavHost}`, `ui/activerental/ActiveRentalScreen`.

- Not (16.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.**

---

### Cüzdan Ekranı (Bakiye + İşlemler + Kart Yönetimi) — `GET /wallet`, `POST /wallet/topup`, `/cards`

- Seçim: **Home'un "Cüzdan" sekmesi (`RencarDestinations.WALLET`) placeholder'dan gerçek MVI ekranına yükseltildi (§4.6).** Ekran: mavi bakiye kartı + **Bakiye Yükle** (ekran içi pop-up, `POST /wallet/topup`, 10–5.000 ₺), **Kayıtlı kartlar** (marka+•••• son4+SKT; "Varsayılan yap" `PATCH /cards/{id}/default`, "Sil" onay pop-up'ıyla `DELETE /cards/{id}`, "+ Ekle" pop-up `POST /cards`), **Son işlemler** (`GET /wallet` → transactions).

- Son Güncelleme Tarihi: 16.07.2026

- Alternatifler: **Bakiye yükleme/kart ekleme için ayrı sayfa** — Ödeme ekranıyla tutarlı kalmak için pop-up (`androidx.compose.ui.window.Dialog`) seçildi.

- Sebep: Tasarımdaki cüzdan ekranı; kullanıcı talebi "bakiye yükle + kart ekle + varsayılan/sil". Kart CRUD zaten Ödeme ekranı için **mevcut** (`CardApi` + `CardUi` + `PaymentMapper` + `PaymentRepository`); tekrar yazılmadı — `WalletViewModel` bu iş için **`PaymentRepository`'yi yeniden kullanır** ("Minimum Değişiklik", kod tekrarı yok). Cüzdan bakiyesi/işlem/yükleme yeni `WalletRepository` ardındadır.

- **DTO izolasyonu korunur (decisions.md "Katman Derinliği"):** `WalletResponse` (PaymentDtos içinde) **additive** olarak `transactions` kazandı (default `emptyList` → ödeme akışının `toBalance()`'ı etkilenmez); yeni `WalletTransaction` + `TopupRequest` DTO'ları eklendi. `WalletResponse` → `WalletUi`/`WalletTransactionUi` **`WalletMapper`** ile çevrilir; `ui/wallet/*` katmanında `data.remote.dto` importu yoktur.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli.

- **Kararlar/sapmalar:**
  - **Göreli tarih (§ "Mahalle/tarih" kalıbı):** işlem tarihi "Bugün · 14:32 / Dün · 09:10 / dd.MM.yyyy · HH:mm" olarak mapper'da üretilir. minSdk 24 + desugaring kapalı → java.time yerine `SimpleDateFormat` ('X' ISO ofseti) + `Calendar` (RentalMapper kalıbı).
  - **Para biçimi:** cüzdan mock'una uygun Türkçe biçim `₺%,.2f` ("₺340,00"); işlem tutarları işaretli ("+₺200,00" / "−₺110,50", `isCredit` renklendirmesi). (Not: Ödeme ekranı `%.2f ₺` dot-biçimi kullanır — iki ekranın mock'ları farklı; her ekran kendi mock'una sadık.)
  - **Tutar aralığı client kapısı:** Yükle butonu 10–5.000 ₺ dışında pasif; sunucu da 400 döner.
  - Açılışta cüzdan + kartlar **paralel** yüklenir; cüzdan kritiktir (başarısızsa tam ekran hata + tekrar dene), kart hatası sessizce boş sayılır.
  - Navigasyon yok — ekran Home sekmesi içinde kendi kendine yeten (`hiltViewModel()`); `RencarDestinations`/`RencarNavHost` değişmedi.

- **Dokunulan/eklenen dosyalar:** yeni `data/model/WalletModels`, `data/mapper/WalletMapper`, `data/repository/WalletRepository`, `ui/wallet/{WalletContract,WalletViewModel,WalletScreen}`; güncellenen `data/remote/dto/PaymentDtos` (WalletResponse.transactions + WalletTransaction + TopupRequest), `data/remote/api/WalletApi` (topup), `ui/icons/RencarIcons` (Plus), `ui/home/HomeScreen` (placeholder → WalletScreen).

- Not (16.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.**