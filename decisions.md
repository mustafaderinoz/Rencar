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


### Kayıt Ekranı (Register) — `POST /auth/register`

- Seçim: **Login'de girilen numara kayıtlı DEĞİLSE (`POST /auth/login` → 401) kullanıcı otomatik olarak yeni `register?phone={phone}` ekranına alınır** (numara taşınır, düzenlenebilir kalır). Alanlar: Ad Soyad · E-posta · Şifre · Telefon (zorunlu) + **Davet kodu (isteğe bağlı)**. Kayıt başarılı olunca API **201 ile token çifti dönse de bu token BİLİNÇLİ OLARAK YOK SAYILIR**; kullanıcı Login'e geri döner ve normal OTP akışıyla giriş yapar. Login'in alt "Kayıt ol" linki de aynı ekranı açar (numara boş gelebilir).

- Son Güncelleme Tarihi: 17.07.2026

- Alternatifler: **Token'ı kaydedip doğrudan Home/Ehliyet'e geçmek** (kayıt sonrası 2. bir adım gerekmez; kullanıcı AÇIKÇA reddetti — telefon OTP ile doğrulanmadan oturum açılmasın). **Kullanıcının tarif ettiği "OTP → kayıtsızsa register" akışı** — backend'de KARŞILIĞI YOK (aşağıya bakınız).

- **Akış sapması (§2.2 uydurmak yasak — kullanıcıya sorulup onaylandı):** İstenen akış "telefon → OTP → kod gir → numara kayıtlı değilse register" idi. Backend bunu **yapamaz**: `POST /auth/login` kayıtsız numaraya **OTP GÖNDERMEZ**, doğrudan **401** döner (canlı v2 ile curl doğrulandı: `{"message":"Bu telefon numarasına kayıtlı kullanıcı bulunamadı."}`). "Telefon kayıtlı mı?" ucu da yoktur. Bu yüzden kayıtsızlık **ancak "Kod Gönder" adımında** anlaşılır ve yeni kullanıcı için OTP adımı **atlanır**. Kayıt sonrası Login'e dönüldüğünde numara OTP'den geçtiği için doğrulama yine de yapılmış olur.

- Sebep: `LoginViewModel` 401'i zaten "Bu telefon numarasına kayıtlı kullanıcı yok." çıkmaz sokağına çeviriyordu ve `LoginScreen`'deki "Kayıt ol" linki no-op'tu; ikisi de bu ekranı bekliyordu. 401 artık hata değil, **kayıt akışına yönlendirme sinyalidir** (`LoginUiState.navigateToRegister`).

- **DTO izolasyonu korunur (decisions.md "Katman Derinliği"):** yeni `RegisterRequest` DTO'su; `AuthRepository.register()` **`Result<Unit>`** döner (token kaydedilmediğinden yanıt kullanılmaz — `login`/`reserve`/`upload` ile aynı kalıp). Hata, **tiplenmiş `RegisterError`** (data/model) olarak `RegisterException` içinde taşınır; HTTP gövdesini ayrıştırma **yalnız mapper katmanındadır** (`AuthMapper.toRegisterError`), `ui/register/*` içinde `data.remote.dto` importu yoktur.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli.

- **Kararlar/sapmalar:**
  - **409 belirsizliği (mapper'ın var oluş sebebi):** E-posta ve telefon çakışmasının **İKİSİ DE 409** döner; **yalnızca gövde metni ayırır** ("Bu e-posta adresi zaten kayıtlı." / "Bu telefon numarası zaten kayıtlı."). Alan-bazlı hata için gövde okunur ve anahtar kelimeyle (`e-posta`/`telefon`) ayrıştırılır. Doğrulama 400'ü de bu kelimeleri içerdiğinden (`"Geçerli bir e-posta adresi giriniz."`) çakışma dalları **`code() == 409` koşuluna bağlıdır**. Sunucu metni değişirse tek dosya (`AuthMapper`) güncellenir.
  - **`message` alanı çift şekilli:** 409/davet-kodu-400'de tek `String`, alan doğrulama 400'ünde `List<String>`. Mapper ikisini de tek listeye indirger; eşlenemeyen 400/409'da sunucunun kendi Türkçe metni gösterilir (metin uydurulmaz).
  - **Davet kodu:** boş/yalnız-boşluk ise `null`'a çevrilir ve `explicitNulls = false` sayesinde gövdeye **hiç yazılmaz** (openapi: geçersiz kod 400 → `"Davet kodu geçersiz."` → `referralCodeError`).
  - **Şifre:** `RegisterDto` zorunlu kılar (min 6) ama giriş parolasız OTP olduğundan **bir daha kullanılmaz** — backend sözleşmesi böyle, uydurma değil. Göz (reveal) ikonu `RencarIcons`'ta ve tasarımda olmadığından **eklenmedi** (§2.2).
  - **Telefon prefill ama kilitli DEĞİL:** "Kayıt ol" linkinden gelindiğinde numara boş olabildiğinden tek davranış (hep düzenlenebilir) seçildi; token yok sayıldığı için yanlış numara zararsız (kullanıcı neyle kaydolduysa onunla giriyor).
  - **Login'e dönüş `popBackStack(LOGIN, inclusive = false)` ile yapılır** (navigate DEĞİL): geri yığındaki Login girdisi korunduğundan `LoginViewModel` yaşar ve **girilen numara alanda kalır**.
  - **"Kaydın tamamlandı" bilgisi — VM'de DEĞİL:** bayrak Login'in `NavBackStackEntry.savedStateHandle`'ına yazılır. Bu handle, Hilt'in VM'e enjekte ettiği `SavedStateHandle` ile **AYNI NESNE DEĞİLDİR** (entry kendi iç `SavedStateViewModel`'ini kullanır) — VM'den okunsa bayrak hiç görünmezdi. Bu yüzden `RencarNavHost` okuyup `LoginScreen`'e parametre geçirir; stateless gövde yalnız `LoginUiState` görmeye devam eder (§4.5).
  - **Telefon alanı ortaklaştırıldı:** `CountryCodeBox` + `PhoneVisualTransformation` `LoginScreen`'den `ui/components/PhoneField`'a taşındı (kopyalanmadı); Login davranışı değişmedi.
  - Navigasyon ekran katmanında (`LaunchedEffect(registered)` → `onRegistered`); VM'de Effect kanalı eklenmedi (§4.6).

- **Dokunulan/eklenen dosyalar:** yeni `data/model/RegisterError`, `ui/components/PhoneField`, `ui/register/{RegisterContract,RegisterViewModel,RegisterScreen}`; güncellenen `data/remote/dto/AuthDtos` (RegisterRequest), `data/remote/api/AuthApi` (register), `data/mapper/AuthMapper` (toRegisterError), `data/repository/AuthRepository` (register), `ui/login/{LoginContract,LoginViewModel,LoginScreen}`, `ui/navigation/{RencarDestinations,RencarNavHost}`.

- Not (17.07.2026): **KOD HİZALANDI — `:app:installDebug` başarılı; akış emülatörde uçtan uca sürüldü.** Doğrulananlar: kayıtsız numara → 401 → Register (numara taşındı) · geçersiz davet kodu → alan hatası (kullanıcı oluşmadı) · kayıt → Login'e dönüş + numara korundu + bilgi satırı · **uygulama yeniden başlatıldığında Onboarding geldi (Home DEĞİL) → token gerçekten kaydedilmiyor** · aynı numarayla giriş → OTP → Ehliyet Doğrulama (PENDING) · aynı e-posta → e-posta alanı hatası · aynı telefon → telefon alanı hatası · geçersiz e-posta/kısa şifre → yerel doğrulama.


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

- Seçim: **CameraX (1.4.2) + ML Kit Face Detection (16.1.7, bundled). Yüz ML Kit ile ~1.2 sn ovalde ortalı kalınca ön kameradan bir selfie karesi `ImageCapture` ile çekilir ve ehliyet ön+arka ile birlikte `POST /license/upload`'a `selfie` part'ı olarak gönderilir.**

- Son Güncelleme Tarihi: 19.07.2026 (ilk: 09.07.2026)

- Alternatifler: **Camera2 API (düşük seviye)**, **ML Kit unbundled (Play Services)**

- Sebep: Selfie ekranında canlı önizleme + gerçek zamanlı yüz ortalama; CameraX lifecycle-bağlı ve Compose (`PreviewView` + `AndroidView`) ile temiz. ML Kit bundled model offline çalışır (Play Services'e bağımlı değil). ML Kit yüz ortalama yalnızca **client-side canlılık/hazır kapısıdır** (çekimi ne zaman tetikleyeceğini belirler); doğrulanan selfie görüntüsünün kendisi backend'e yüklenir.

- **BUG düzeltmesi (19.07.2026) — "Fotoğraf geçersiz, ehliyet adımına dönüp tekrar çekin." (400):** Eski karar (09.07) "selfie backend'e GÖNDERİLMEZ; `UploadLicenseDto` yalnız front+back alır" diyordu. Backend **D5** ile selfie'yi zorunlu kıldı: `UploadLicenseDto.required = [front, back, selfie]` (17.07.2026'da canlı sunucudan tazelenen `openapi.json` ile doğrulandı — bkz. "Canlı Sunucudan Tazeleme"). İstemci hâlâ yalnız 2 dosya gönderdiğinden sunucu **her** yüklemede 400 dönüyordu; `SelfieViewModel` bunu genel "Fotoğraf geçersiz…" mesajına çeviriyordu (yüz aslında tanınıyordu — 400 ancak yüz ortalanıp yükleme tetiklendikten SONRA gelir). Selfie ekranı hiçbir kare çekmiyordu (yalnız `Preview` + `ImageAnalysis` bağlıydı). Düzeltme: `CameraPreview`'e `ImageCapture` use-case'i eklendi; hold dolunca `captureRequested` state'i ile çekim tetiklenir, kare `filesDir/licenses/selfie.jpg`'e yazılır ve sonuç `SelfieCaptured`/`SelfieCaptureFailed` intent'iyle döner (Android-API/leaf tetikleyici — §4.5). Selfie de front/back gibi ortak `toImagePart` ile 5MB altına sıkıştırılır.

- **Yeni bağımlılık YOK.** `ImageCapture` mevcut `androidx.camera:camera-core`'un parçası (Preview/ImageAnalysis ile aynı artifact).

- **Dokunulan dosyalar:** `data/remote/api/LicenseApi` (upload'a selfie part), `data/repository/LicenseRepository` (upload(front,back,selfie)), `ui/selfie/{SelfieContract,SelfieViewModel,SelfieScreen}` (captureRequested state + SelfieCaptured/SelfieCaptureFailed intent + ImageCapture bağlama/tetik).


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

> **Güncelleme (17.07.2026):** Ekrana üçüncü bir yöntem eklendi — bkz. "İyzico ile Ödeme (Checkout Form + WebView)".

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

---

### `docs/api/openapi.json` — Canlı Sunucudan Tazeleme (17.07.2026)

- Seçim: **`docs/api/openapi.json` canlı Swagger'dan (`/api/docs/swagger-ui-init.js` içindeki gömülü `swaggerDoc`) yeniden üretilir; dosya sunucunun gerisinde kalmışsa entegrasyon öncesi TAZELENİR.**

- Son Güncelleme Tarihi: 17.07.2026

- Sebep: İyzico işine başlarken dosyada iyzico uçları YOKTU ve `PayRentalDto.method` yalnız `WALLET | CARD` idi; canlı sunucuda ise 8 iyzico ucu + `IYZICO` enum'u mevcuttu. AGENTS §2.2 ("uydurmak yasak") gereği eksik sözleşme uydurulamayacağından dosya canlı gerçekle hizalandı: **41 → 49 path, 43 → 52 şema.** Bu, v1 → v2 geçişindeki durumun tekrarıdır (bkz. "API Base URL" notu) — dosyanın bayat olabileceği varsayılmalı, iş öncesi doğrulanmalıdır.

- **Üretim yöntemi (tekrarlanabilir):** `swagger-ui-init.js` indirilir, gömülü `swaggerDoc` nesnesi ayrıştırılır, `JSON.stringify(spec, null, 2)` ile yazılır; ardından mevcut dosyanın konvansiyonu korunur: `>` → `>` kaçışı, **CRLF** satır sonu, sonda newline YOK. (`/api/docs-json` gibi bir JSON ucu YOKTUR — 404.)

- Not: `"contact": {}` artık tek satır. Eski dosyadaki `{\n\n}` biçimi sunucudan gelmiyordu (ham çıktı da `{}` veriyor); kozmetik sapma giderildi.

---

### İyzico ile Ödeme (Checkout Form + WebView) — `POST /rentals/{id}/pay` (method=IYZICO)

- Seçim: **Ödeme ekranına üçüncü yöntem olarak İyzico eklendi (Cüzdan / Kart / İyzico segmenti). Akış İyzico'nun ORTAK ÖDEME SAYFASI'dır (Checkout Form): `POST /iyzico/checkout-form/initialize` → dönen `paymentPageUrl` uygulama içi WebView'da açılır → `WebViewClient` sunucunun callback adresini görünce katman kodsal olarak kapatılır → `GET /iyzico/checkout-form/result/{token}` ile sonuç doğrulanır → `POST /rentals/{id}/pay` (`method=IYZICO`, `iyzicoPaymentId`) ile kiralama PAID işaretlenir.**

- Son Güncelleme Tarihi: 17.07.2026

- Alternatifler: **Doğrudan kart ödemesi non-3DS** (`POST /iyzico/payments`) ve **3DS başlatma** (`POST /iyzico/payments/threeds/initialize`, `threeDSHtmlContentDecoded` WebView'a yüklenir) — ikisi de kart numarası/CVV'yi UYGULAMADA toplamayı gerektirir. Checkout Form seçildi: kart verisi hiçbir zaman istemcinin sorumluluğunda olmaz (PCI kapsamı dışı kalır) ve tasarımdaki ekranlar bu sayfayı gösterir.

- Sebep: Cüzdan/Kart yöntemleri sunucuda SİMÜLE edilir; İyzico **gerçek tahsilattır** (sandbox). Kütüphane/dış sistem kullanımı repository ardında tutulur (`PaymentRepository.startIyzicoCheckout/verifyIyzicoCheckout/payWithIyzico`); UI yalnız `IyzicoCheckoutUi`/`IyzicoVerificationUi` modellerini görür.

- **Yeni bağımlılık YOK.** `android.webkit.WebView` framework'ün parçasıdır (`AndroidView` ile sarılır); `androidx.webkit` gerekmedi. `INTERNET` izni zaten mevcut.

- **DTO izolasyonu korunur (bkz. "Katman Derinliği"):** yeni `IyzicoDtos` → `IyzicoModels`, `IyzicoMapper` ile çevrilir; `ui/payment/*` içinde `data.remote.dto` importu yoktur. `PayRentalRequest`'e `iyzicoPaymentId` **additive + nullable-default** eklendi; `explicitNulls=false` sayesinde mevcut WALLET/CARD gövdeleri DEĞİŞMEDİ ("Minimum Değişiklik").

- **Kararlar/sapmalar:**
  - **`basketId` sözleşmesi:** `POST /rentals/{id}/pay` (IYZICO) doğrulaması sepet kimliğinin **`rental-<kiralamaId>`** olmasını şart koşar. Bu ayrıntı `PaymentRepository`'de kapsüllendi (UI bilmez). Hocanın dersteki "rastgele UID üret" önerisi bu backend'de GEÇERSİZDİR — doğrulama başarısız olur.
  - **Callback adresi:** gerçek yol `<BASE_URL>/iyzico/checkout-form/callback` (ders notundaki `/easico/checkoutform/callback` yaklaşıktı). Uç **public**tir ve HTML sonuç sayfası döndürür; curl ile doğrulandı.
  - **Callback yakalama `onPageStarted` ile yapılır, `shouldOverrideUrlLoading` ile DEĞİL:** İyzico callback'e tarayıcıdan **POST** ile döner ve `shouldOverrideUrlLoading` POST navigasyonlarında çağrılmaz. Çift tetiklenme `notified` bayrağıyla engellenir.
  - **Doğrulama sunucu callback'ini BEKLEMEZ:** `GET /iyzico/checkout-form/result/{token}` durumu doğrudan İyzico'dan okur, bu yüzden WebView'ı `onPageStarted`'da (POST hâlâ uçarken) kapatmak yarış koşulu yaratmaz.
  - **İndirim kodu İyzico'da YOK:** API `method=IYZICO` + `discountCode` birlikte gelirse 400 döner. Alan İyzico seçiliyken **gizlenir** (`isDiscountAvailable`); `payWithIyzico` kod parametresi almaz. Yöntem değişince girilmiş kod silinmez, yalnız gönderilmez.
  - **Tutar kapısı:** `InitializeCheckoutFormDto.price` 1–100.000 TL. Aralık dışında "Öde" pasifleşir + uyarı (`iyzicoAmountOutOfRange`); sunucu da 400 döner.
  - **İptalde de doğrulama yapılır (güvenlik ağı):** kullanıcı sayfayı ✕/geri ile kapatırsa (`IyzicoDismissed`) sonuç **sessizce** sorgulanır — callback yakalanamadan kapatılan başarılı bir ödeme aksi hâlde tahsil edilip kiralamaya işlenmeden kalırdı. Başarısızsa hata GÖSTERİLMEZ (kullanıcı zaten vazgeçti); `IyzicoCallbackReached`'te ise gösterilir.
  - **Tahsilat sonrası `pay` hatası gizlenmez:** İyzico'da ödeme SUCCESS olup `POST /rentals/{id}/pay` başarısız olursa mesaj paranın çekildiğini açıkça söyler (`toIyzicoPayMessage`).
  - **`buyer` alanı gönderilmez** (§2.2): sunucu alıcı bilgisini kullanıcı kaydından doldurur; adres/TCKN uydurulmaz.
  - **WebView ayrı sayfa değil, ekran içi tam ekran katman** — Kart Ekle/Sil pop-up'larıyla aynı kalıp. Uzun `paymentPageUrl`'i route argümanına encode etmek gerekmez; `key(paymentPageUrl)` ile yeni oturumda WebView baştan kurulur, `onRelease`'te `stopLoading` + `destroy` ile sızıntı önlenir. Geri tuşu ✕ ile aynı davranır.
  - **Kendi çerçevemiz (17.07.2026):** başlık çubuğu (kilit rozeti + "Kart bilgileriniz uygulamada tutulmaz"), sayfa açılırken boş beyaz WebView'ı örten **yükleniyor katmanı**, ana çerçeve hatalarında **"Tekrar dene"** (`onReceivedError` + `isForMainFrame`; alt kaynak hataları yok sayılır), ve ✕/geri için **kapatma onayı** (yanlışlıkla yarım kalan ödemeyi önler).
  - **Sandbox 3DS sayfası — DEBUG'da stil enjeksiyonu (17.07.2026, KARAR DEĞİŞTİ):** Katmanın içindeki sayfa İyzico'ya (3DS adımında bankaya) aittir. Önce "hiç dokunulmaz" kararı alınmıştı; kullanıcı sandbox simülatörünün ham görünümünü (biçimsiz "Sms Code + yeşil Submit / kırmızı Cancel") demo için kabul edilemez bulduğundan karar **sınırlı enjeksiyon** lehine değiştirildi (`SANDBOX_3DS_RESTYLE_JS`, `onPageFinished`).
    - **İki bağımsız kapı:** (a) yalnız `BuildConfig.DEBUG` — release'de hiç çalışmaz, dolayısıyla production'daki GERÇEK banka 3DS sayfasına asla dokunulmaz; (b) **kendi kendini hedefler** — script yalnız metni tam "submit" ve "cancel" olan iki eylem öğesi bulursa çalışır, bulamazsa (İyzico'nun kendi ödeme formu, gerçek banka sayfası) no-op'tur.
    - **CSS sınıf adı VARSAYILMAZ (§2.2):** sayfanın HTML'i elimizde olmadığından (3DS URL'i ödeme başına üretilir, önden çekilemez) butonlar sınıfla değil **görünen metinleriyle** bulunur.
    - **Form bağı korunarak taşınır:** butonlar yan yana dizilebilmek için tek bir flex satırına alınır. Ayrı `<form>`'larda olabileceklerinden, taşımadan ÖNCE her butona HTML5 **`form` attribute'ü** yazılır (forma id yoksa üretilir) — böylece buton formun dışına çıksa da gönderim bağı kopmaz. Taşıma `try/catch` içindedir: başarısız olursa butonlar yerinde kalır ve taban stille alt alta görünür.
    - **Kırılganlık kabul edildi:** İyzico bu sayfanın metinlerini/yapısını değiştirirse enjeksiyon sessizce devre dışı kalır — sayfa ÇALIŞMAYA devam eder, yalnız ham görünür (ödeme akışı etkilenmez).
    - **Doğrulama:** enjekte edilen script Kotlin derleyicisinin denetiminden geçmez (ham string). Node ile ayrıştırılıp sahte DOM üzerinde üç senaryoda sınandı: (a) Submit/Cancel ayrı formlarda → yan yana + her butona kendi form id'si bağlandı, (b) tek formda → yan yana, mevcut id korundu, (c) alakasız sayfa → no-op.
  - **Katmanın yerel görünüm durumu** (yükleniyor/hata/çıkış onayı) ViewModel'e taşınmadı: intent'lerle sürülen uygulama state'i değil, WebView'ın kendi sayfa-yükleme durumudur (§4.2 "saf UI durumu" ile uyumlu; VM'e taşımak her sayfa olayı için intent gerektirirdi).
  - **Kart bilgisi uygulamada toplanmaz:** JS + DOM storage yalnız İyzico/banka sayfası için açıktır.
  - Navigasyon değişmedi (`RencarDestinations`/`RencarNavHost` aynı); §4.6 gereği Effect kanalı eklenmedi.

- **Sandbox test bilgileri:** kart `5528790000000008` · 12/2030 · CVC 123 — 3D Secure SMS kodu **283126**.

- **API anahtarı (backend işi):** ödemeler backend'deki firma anahtarına düşer. Kendi İyzico Sandbox panelinizde görmek için backend'in firma ayarı sizin API anahtarınızla güncellenmelidir — mobil taraftan yapılamaz. `GET /iyzico/health` anahtarların ayarlı olduğunu doğrular (auth ister); anahtar yoksa tüm iyzico uçları **503** döner ve ekran "Ödeme sağlayıcı şu anda kullanılamıyor" gösterir.

- **Dokunulan/eklenen dosyalar:** yeni `data/remote/dto/IyzicoDtos`, `data/remote/api/IyzicoApi`, `data/model/IyzicoModels`, `data/mapper/IyzicoMapper`, `ui/payment/IyzicoCheckoutWebView`; güncellenen `data/remote/dto/PaymentDtos` (PayRentalRequest.iyzicoPaymentId), `data/repository/PaymentRepository`, `di/NetworkModule` (IyzicoApi), `ui/payment/{PaymentContract,PaymentViewModel,PaymentScreen}`, `ui/icons/RencarIcons` (Close), `docs/api/openapi.json`.

- Not (17.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.** Uçtan uca sandbox ödemesi cihazda DENENMEDİ (giriş yapmış CUSTOMER oturumu gerekir).

---

### Günlük (DAILY) Kiralama Akışı — `POST /rentals` + `endDate`

- Seçim: **Günlük planda kiralama REZERVASYON EKRANINDA açılır (`POST /rentals`, `plan=DAILY`, `endDate` = şu an + 1 gün) ve doğrudan Aktif Yolculuk ekranına geçilir; oradan "Kiralamayı Bitir" → Ödeme.** Foto adımı YOKTUR (API bu planda kaydı anında ACTIVE yapar).

- Son Güncelleme Tarihi: 17.07.2026

- Sebep: **Günlük plan hiç uygulanmamıştı.** `RencarNavHost`'ta DAILY dalı `popBackStack()` ile Home'a dönüyordu ve `POST /rentals` yalnız `RentalPhotosViewModel`'den (yani yalnız PER_MINUTE/HOURLY'de) çağrılıyordu. Sonuç: kullanıcı günlük rezerve edince araç 15 dk tutuluyor, kiralama hiç açılmıyor, rezervasyon sessizce düşüyordu; ödeme ekranına da ulaşılamıyordu. Hata mesajı çıkmadığından bozukluk görünmüyordu. `CreateRentalRequest`'te `endDate` alanı da yoktu (yorumu bunu kapsam dışı ilan ediyordu) — yani mevcut `createRental` günlük için çağrılsa API 400 dönerdi.

- **Gün sayısı — sabit 1 gün (kullanıcı kararı):** Alternatif "1–30 gün seçici" idi. Rezervasyon ekranı zaten `RentalPlan.DAILY.estimateLabel = "1 gün"` gösteriyor ve fiyat önizlemesini 1440 dk üzerinden alıyor; `endDate`'i buna eşitlemek **ekranda görünen tutar ile faturalanan tutarın birebir eşleşmesini** sağlar ve yeni UI gerektirmez. Gün seçici ileride eklenirse yalnız `RentalRepository.DAILY_RENTAL_DAYS` + quote `minutes` parametresi değişir.

- **DTO ("Minimum Değişiklik"):** `CreateRentalRequest`'e `endDate: String? = null` **additive + nullable-default** eklendi. `explicitNulls=false` sayesinde Dakikalık/Saatlik gövdelerine yazılmaz (API bu planlarda `endDate` verilirse 400 döner) — mevcut foto akışı DEĞİŞMEDİ.

- **Tarih üretimi repository'de kapsüllendi:** `RentalRepository.createDailyRental(vehicleId)` hem gün sayısını hem ISO biçimini bilir; UI'a sızmaz (iyzico `basketId` kalıbının aynısı). minSdk 24 + desugaring kapalı olduğundan java.time yerine `SimpleDateFormat`/`Calendar` + `Locale.US` + UTC kullanıldı (mapper katmanı kalıbı). Biçim `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` (openapi örneği: `2026-07-15T10:00:00.000Z`). JBR ile çalıştırılan bir denemede doğrulandı: biçim eşleşmesi, tarihin gelecekte olması (+24 sa), cihaz `Europe/Istanbul` iken bile UTC yazması ve `ar-EG` yerel ayarında ASCII rakam üretmesi (`Locale.US` sayesinde).

- **Kararlar/sapmalar:**
  - Rezervasyon + kiralama **tek kullanıcı işlemi** sayılır: buton spinner'ı (`isReserving`) zincir bitene kadar açık kalır.
  - `POST /rentals` başarısız olursa rezervasyon askıda kalır (araç 15 dk RESERVED). Hata metni ayrı eşlenir (`toStartRentalMessage`); kullanıcı tekrar denerse `POST /reservations` 409 döner ve o mesaj görünür. **Otomatik rezervasyon iptali EKLENMEDİ** — istenirse `DELETE /reservations/{id}` ile ayrı bir iş.
  - Navigasyon §4.6'ya uygun: Effect kanalı yok, state bayrağı (`startedRentalId`) + ekran katmanında `LaunchedEffect`.
  - `POST /rentals/{id}/return` (eski uç, yalnız DAILY) KULLANILMAZ: `finish` günlük planda da çalışır ve baştan kilitlenen fiyatı değiştirmez — böylece bitirme + ödeme akışı tüm planlarda TEKTİR (iyzico dahil).

- **Dokunulan dosyalar:** `data/remote/dto/RentalDtos` (CreateRentalRequest.endDate), `data/repository/RentalRepository` (createDailyRental + isoUtcFromNow), `ui/reservation/{ReservationContract,ReservationViewModel,ReservationScreen}`, `ui/navigation/RencarNavHost`.

- Not (17.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.** Uçtan uca cihazda DENENMEDİ.

---

### Çıkış Yap (Logout) — `POST /auth/logout`

- Seçim: **Profil ekranındaki "Çıkış yap" butonu onay pop-up'ıyla aktifleştirildi: onaylanınca `POST /auth/logout` (sunucudaki aktif refresh oturumlarını iptal eder) çağrılır, ardından YEREL oturum kapatılır ve kullanıcı Login'e döner (tüm backstack temizlenir).** Yerel oturum kapatma + Login'e yönlendirme, mevcut `SessionManager.forcedLogout` → `MainViewModel` → `RencarNavHost` zinciri YENİDEN KULLANILARAK yapılır — navigasyon tarafında sıfır değişiklik ("Minimum Değişiklik İlkesi").

- Son Güncelleme Tarihi: 17.07.2026

- Alternatifler: **Navigasyon callback'ini `RencarNavHost` → `HomeScreen` → `ProfileScreen` boyunca geçirmek** — 3 ekstra dosyaya dokunur, `ProfileScreen`'in (WalletScreen gibi) kendi kendine yeten yapısını bozar ve `forcedLogout` LaunchedEffect'inin zaten yaptığı navigasyonu (Login + `popUpTo(graph.id){inclusive}`) tekrarlardı. **Onay pop-up'sız doğrudan çıkış** — kullanıcı onay pop-up'ı istedi (kart-silme kalıbı).

- Sebep: `ProfileScreen`, Home'un nested tab NavHost'unda navigasyon callback'i ALMADAN çağrılır; kök NavHost'taki Login'e doğrudan erişimi yoktur. `forcedLogout` olayının semantiği zaten "oturum bitti → Login'e git, backstack temizle"dir (refresh-hatası yalnızca BİR tetikleyici); kullanıcı-tetikli çıkış ikinci tetikleyici olarak aynı yolu kullanır.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + Hilt + Compose yeterli; yeni DTO da yok.

- **Kararlar/sapmalar:**
  - **Ağ çağrısı `AuthApi` (ana istemci) üzerinden, `RefreshApi` DEĞİL:** `/auth/logout` access token ister; `RefreshApi` Authenticator'sız + token eklemeyen sade istemcidir (DI-döngü çözümü). Token'ı `AuthInterceptor` yalnız ana istemcide ekler.
  - **"Her durumda çıkış":** `AuthRepository.logout()` içinde sunucu çağrısı `runCatching{...}.also { sessionManager.logout() }` ile sarılır — ağ başarısız olsa BİLE yerel oturum (token temizle + olay) kapatılır; kullanıcı çıkar. Sunucuya ulaşılamazsa eldeki refresh token kısa ömrü dolunca zaten çürür. Yanıt gövdesi (`MessageResponseDto`) kullanılmaz → `Response<Unit>` (CardApi.remove kalıbı) + `Result<Unit>`.
  - **DI döngüsü yok:** `AuthRepository` artık `SessionManager` enjekte eder; `SessionManager` yalnız `RefreshApi`+`TokenStore`'a bağlıdır (AuthRepository/AuthApi'ye geri bağlanmaz). Hilt derlemede doğruladı.
  - **`SessionManager.logout()` (public) eklendi:** mevcut private `forceLogout()` gövdesini (clear + emit) yeniden kullanır; iki tetikleyici (kullanıcı çıkışı / refresh-hatası) tek yerel-kapatma + tek navigasyon yoluna iner.
  - **Onay pop-up'ı ViewModel state'inde (`showLogoutConfirm`), yerel `remember` DEĞİL:** stateless gövde (§4.5) saf kalır (yalnız `uiState`+`onIntent`); WalletScreen `DeleteCardDialog` kalıbıyla tutarlı. Çıkış sürerken (`isLoggingOut`) onay butonunda spinner + tekrar-basma/dışına-dokunma engeli.
  - Navigasyon §4.6'ya uygun: `ProfileViewModel`'e Effect kanalı EKLENMEDİ; oturum-sonu olayını NavHost dinler, ekran zaten backstack'ten çıkar.

- **Dokunulan/eklenen dosyalar:** güncellenen `data/remote/api/AuthApi` (logout), `data/remote/session/SessionManager` (public logout), `data/repository/AuthRepository` (logout + SessionManager inject), `ui/profile/{ProfileContract,ProfileViewModel,ProfileScreen}` (Logout intent/state + onay pop-up).

- Not (17.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.** Uçtan uca cihazda DENENMEDİ (giriş yapmış oturum gerekir).

---

### Kiralamalarım Ekranı (Geçmiş sekmesi) — `GET /rentals` + `GET /rentals/stats`

- Seçim: **Home'un "Geçmiş" sekmesi (`RencarDestinations.HISTORY`) placeholder'dan gerçek MVI ekranına yükseltildi (§4.6).** Ekran: başlık ("Kiralamalarım") + aylık özet ("Bu ay N yolculuk · ₺X harcama", `GET /rentals/stats`) + kiralama kartları listesi (`GET /rentals`, yeniden eskiye). Her kart: dekoratif rota küçük görseli + araç adı + tarih + süre/mesafe rozetleri + tutar.

- Son Güncelleme Tarihi: 18.07.2026

- Alternatifler: **Yalnız COMPLETED kiralamaları göstermek** — kullanıcı "yapılan her yeni kiralama buraya eklenmeli" dediğinden tüm durumlar gösterilir. **Başlığı istemcide listeden türetmek** — kullanıcı başlığın `GET /rentals/stats` ile beslenmesini AÇIKÇA istedi (openapi de bu başlığı bu uca bağlar).

- Sebep: Tasarımdaki Kiralamalarım ekranı. `GET /rentals` tüm kiralamaları (PREPARING/ACTIVE/COMPLETED/CANCELLED) yeniden eskiye döner; tutarı kilitlenmiş (tamamlanmış) kayıtta tutar ("₺110,50"), tutarı olmayan kayıtta durum etiketi ("Aktif"/"Hazırlanıyor"/"İptal edildi") gösterilir — böylece yeni açılan kiralamalar da listede belirir.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli.

- **DTO izolasyonu korunur (bkz. "Katman Derinliği"):** `RentalResponse`'a `startedAt`/`createdAt` (kart tarihi) **additive + nullable-default** eklendi; yeni `RentalStatsResponse` DTO'su eklendi. `RentalResponse` → `RentalHistoryItemUi`, `RentalStatsResponse` → `RentalStatsUi` **`RentalMapper`** ile çevrilir; `ui/rentals/*` içinde `data.remote.dto` importu yoktur. Para biçimi mevcut `formatTl` (WalletMapper, `internal`) yeniden kullanıldı — kart tutarı "₺110,50", başlık harcaması `formatTlWhole` ile kuruşsuz "₺612".

- **Kararlar/sapmalar:**
  - **Rota küçük görseli DEKORATİFtir (§2.2):** API'de kiralamaya ait rota/polyline yoktur; kartın solundaki mavi güzergâh + uç noktaları Canvas ile temsilî çizilir (mahalle adı / Cüzdan marka çizimi emsalleri). Gerçek yol olduğu iddia edilmez.
  - **Sekmeye her girişte tazeleme:** yükleme `init`'te değil `RentalsIntent.Load` ile; stateful sarmalayıcıdaki `LaunchedEffect(Unit)` sekme her göründüğünde tetikler. Listede veri varken tazeleme SESSİZDİR (spinner çakması yok); yalnız ilk açılışta (veri yokken) tam ekran spinner. Böylece başka akışta tamamlanan kiralama sekmeye dönünce görünür.
  - **Kritik/ikincil ayrımı:** liste kritiktir (başarısızsa tam ekran hata + tekrar dene); aylık özet ikincildir (hatası sessizce yok sayılır, eldeki değer korunur) — Cüzdan ekranındaki cüzdan/kart ayrımıyla aynı kalıp.
  - **Tarih biçimi:** kart "26 Haz 2026 · 14:32" (Türkçe ay kısaltması, `Locale("tr")`), tercihen `startedAt`, yoksa `createdAt`. minSdk 24 + desugaring kapalı → `SimpleDateFormat` (RentalMapper kalıbı; `parseIso` ortaklaştırıldı).
  - Navigasyon yok — ekran Home sekmesi içinde kendi kendine yeten (`hiltViewModel()`); `RencarDestinations`/`RencarNavHost` değişmedi. §4.6 gereği Effect kanalı eklenmedi.

- **Dokunulan/eklenen dosyalar:** yeni `ui/rentals/{RentalsContract,RentalsViewModel,RentalsScreen}`; güncellenen `data/remote/dto/RentalDtos` (RentalResponse.startedAt/createdAt + RentalStatsResponse), `data/remote/api/RentalApi` (listMine + stats), `data/model/RentalUi` (RentalHistoryItemUi + RentalStatsUi), `data/mapper/RentalMapper` (toHistoryItem + stats toUi + tarih/km biçimleri), `data/repository/RentalRepository` (getMyRentals + getMonthlyStats), `ui/home/HomeScreen` (placeholder → RentalsScreen).

- Not (18.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.** Uçtan uca cihazda DENENMEDİ (giriş yapmış CUSTOMER oturumu gerekir).

---

### AI Araç Önerisi (Gemini) — Doğal Dil Filtresi

- Seçim: **Harita ekranındaki AI diyaloğu, kullanıcının doğal dildeki isteğini (`AiRecommendationDialog`) Google Gemini'ye (`com.google.ai.client.generativeai`) gönderir; model, haritadaki müsait araç listesinden EN UYGUN olanların ID'lerini bir JSON dizisi olarak döndürür; bu ID'ler haritada vurgulanır (`MapUiState.recommendedVehicleIds`).** Eşleştirme (lüks→COMFORT, arazi→SUV, bütçe filtresi vb.) tamamen istemci-prompt kurallarıyla yapılır — RenCar API'sinde doğal-dil/öneri ucu YOKTUR (§2.2 uydurmak yasak).

- Son Güncelleme Tarihi: 19.07.2026 (özellik: `feature/gemini` merge'ü; katman hizalaması: 19.07.2026)

- Alternatifler: **Sunucu tarafı öneri ucu** (API'de yok — §2.2), **istemci tarafı elle kural motoru** (doğal dili yorumlayamaz; segment/bütçe/kasa-tipi kombinasyonlarını serbest metinden çıkaramaz).

- Sebep: Tasarımdaki "kendi cümlelerinle anlat, filtreleyelim" akışı. Model konum verisini bu bağlamda kullanmaz (araç konumu prompt'a girmez); yalnız fiyat/segment/tip/koltuk/vites kriterlerine bakar.

- **Katman hizalaması (19.07.2026) — decisions.md "Katman Derinliği" + "Kütüphane" ile uyum:** Özellik `feature/gemini` ile eklendiğinde `AiRepository` SDK'yı gövdesinde kuruyor ve JSON yanıtını inline ayrıştırıyordu (diğer repository'lerin izlediği api/di + mapper disiplininin DIŞINDA). Hizalandı:
  - **Kütüphane DI ardına alındı:** `GenerativeModel` artık `di/AiModule` içinde `@Provides @Singleton` ile sağlanır ve `AiRepository`'ye enjekte edilir (NetworkModule'ün OkHttp/Retrofit sağlaması gibi). Model yapılandırması tek noktada; repo sahte modelle test edilebilir.
  - **Dönüşüm mapper katmanına alındı:** araç betimi (`VehicleUi.toPromptLine()`) ve yanıt ayrıştırma (`parseRecommendedIds`) `data/mapper/AiMapper`'a taşındı. Repository yalnızca eşleştirme kurallarını (prompt) ve model çağrısını orkestre eder — `RentalRepository`'nin tarih/`basketId` iş kuralını kapsüllemesiyle aynı desen (repo-içi iş kuralı bu projede kabul edilir; **+1 UseCase tier EKLENMEZ**, satır ~173).

- **Yeni bağımlılık YOK.** Gemini SDK (`com.google.ai.client.generativeai`) ve `BuildConfig.GEMINI_API_KEY` zaten mevcut.

- **Bilinen/kabul edilen sapmalar:**
  - **Model adı `gemini-3.5-flash` korundu** — mevcut çalışan yapılandırma; §2.2 gereği "düzeltme" adına değiştirilmedi.
  - **Dönüş tipi `Result<List<String>>` (ham ID listesi):** sonuç zaten bir ID kümesidir (`recommendedVehicleIds: Set<String>` olarak tüketilir); tiplenmiş bir sarmalayıcı model eklemek `AiRecommendationViewModel`'i de değiştirirdi ve değer katmazdı ("Minimum Değişiklik").
  - **`AiRecommendation` ikinci bir Contract+ViewModel olarak `ui/map` altında yaşar:** diyalog kendi VM'ine sahiptir; sonuç `AiRecommendationDialog.onApply` → `MapIntent.SetAiRecommendations` ile MapVM'e döner. Diyaloğun stateless içeriği §4.5'e (`uiState`+`onIntent`) **hizalandı (19.07.2026):** aday araç listesi artık `Submit` payload'ı değil, `VehiclesProvided` intent'iyle VM state'ine akar (tek doğruluk kaynağı); kapatma `Dismiss` intent'iyle Screen katmanında ele alınır.

- **Dokunulan/eklenen dosyalar:** yeni `di/AiModule`, `data/mapper/AiMapper`; güncellenen `data/repository/AiRepository` (GenerativeModel inject + mapper'a devir). `ui/map/AiRecommendation*` ve `MapViewModel`/`MapContract` bu adımda DEĞİŞMEDİ.

- Not (19.07.2026): Katman hizalaması yapıldı; derleme doğrulaması kullanıcı ortamında alınacak.

---

### Araç Teslim Fotoğraf Ekranı (kiralama sonrası · ödeme öncesi) — MOCK

- Seçim: **"Kiralamayı Bitir" (`POST /rentals/{id}/finish`) BAŞARILI olduktan sonra doğrudan Ödeme ekranına geçilmez; araya yeni `rental_return_photos/{rentalId}` ekranı girer.** Ekran, kiralama öncesi `ui/rentalphotos/*` ekranıyla tasarım ve işlev olarak aynıdır (2×2 Ön/Arka/Sol/Sağ ızgarası, "N / 4 çekildi" sayacı, yeşil onay rozeti, kesikli boş kart, sarı uyarı şeridi); yalnız metinler teslim akışına uyarlanmıştır ("Araç teslim durumu" / "Teslimden önce 4 yönü çek") ve alt buton **"Ödeme Ekranına Geç"**tir (eksikken pasif + "· N foto kaldı"). 4 yön tamamlanınca `payment/{rentalId}` açılır.

- Son Güncelleme Tarihi: 19.07.2026

- **MOCK — fotoğraflar sunucuya GÖNDERİLMEZ (§2.2 uydurmak yasak; kullanıcıya sorulup onaylandı):** openapi.json'da **teslim fotoğrafı ucu YOKTUR**. Var olan `POST /rentals/{id}/photos` yalnız **PREPARING** aşaması içindir ve sonrasında **409** döner ("Yolculuk PREPARING aşamasında değil"). Bu yüzden yeni ekran ağ çağrısı yapmaz: çekilen kareler `filesDir/rental-return-photos/` altında tutulur, sayaç yerel state'ten ilerler. Uç eklendiğinde değişiklik **tek noktadadır** (ViewModel'e repository çağrısı); ekran/tasarım/navigasyon aynı kalır.

- **Finish sırası — önce finish, sonra foto (alternatif reddedildi):** Ücret `finish` ile o anda kilitlendiğinden foto çekerken geçen süre **faturalanmaz**; bu, başlangıç akışındaki "foto çekerken geçen süre faturalanmaz" (`POST /rentals/{id}/start` → `startedAt` o anda atılır) ilkesiyle tutarlıdır. Reddedilen alternatif: foto ekranındaki butonun finish'i çağırması — kiralama foto boyunca ACTIVE kalır ve o süre faturaya eklenirdi.

- **Yeni bağımlılık YOK.** Kamera, `ui/rentalphotos` ile aynı kalıpla harici çekimdir (`ActivityResultContracts.TakePicture` + `FileProvider`; CameraX kullanılmaz — bkz. "Ehliyet Görsel Kaynağı").

- **Kararlar/sapmalar:**
  - **Enum kopyalanmadı, ayrıştırıldı:** yeni `ReturnPhotoSide`, `rentalphotos.PhotoSide`'dan ayrıdır — orada `apiValue` (POST `side` parametresi) vardır, burada yükleme olmadığı için yoktur. Ortak bir enum, var olmayan bir API sözleşmesini teslim akışına da taşırdı.
  - **Araç özeti nav argümanıyla taşınır** (`?vehicleTitle=…&vehiclePlate=…`, boş olabildiği için path değil query + `defaultValue = ""`): `ActiveRentalUiState` bu bilgiyi zaten tutuyor; ekstra `GET /rentals/{id}` çağrısı eklenmedi ("Minimum Değişiklik"). ViewModel bu yüzden **repository almaz** — saf yerel state.
  - **Kart spinner'ı yok:** yükleme adımı olmadığından `rentalphotos`'taki `uploadingSide`/hata dalları taşınmadı (mock'ta karşılığı yok).
  - **Geri yığını:** `active_rental` → teslim foto geçişinde `popUpTo(ACTIVE_RENTAL) { inclusive = true }`, teslim foto → `payment` geçişinde `popUpTo(RENTAL_RETURN_PHOTOS) { inclusive = true }`. Finish tek yönlü olduğundan ödemeden geri → Home.
  - Navigasyon ekran katmanındadır (`ActiveRentalScreen` `LaunchedEffect(isFinished)` → `onNavigateToReturnPhotos`); VM'de Effect kanalı eklenmedi (§4.6).

- **Dokunulan/eklenen dosyalar:** yeni `ui/rentalreturnphotos/{RentalReturnPhotosContract,RentalReturnPhotosViewModel,RentalReturnPhotosScreen}`; güncellenen `ui/navigation/RencarDestinations` (RENTAL_RETURN_PHOTOS rotası + `rentalReturnPhotosRoute`), `ui/navigation/RencarNavHost` (yeni composable + ActiveRental geçişi), `ui/activerental/ActiveRentalScreen` (`onNavigateToPayment` → `onNavigateToReturnPhotos`; "Ödemeye yönlendiriliyorsunuz…" metinleri teslim adımına göre güncellendi).

- Not (19.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.** Emülatör/cihaz doğrulaması yapılmadı.

---

### Merkezî Hata Yönetimi (`util/AppError.kt` + `util/ErrorMessages.kt`)

- Seçim: **Hiçbir ViewModel hata mesajını elle (hard-coded string) üretmez.** Ham `Throwable`'lar tek tip bir modele (`AppError`) indirgenir, kullanıcı metni tek bir yerden — `util/ErrorMessages.kt` — çözülür. ViewModel'de kullanım tek satırdır:
  ```kotlin
  errorMessage = e.toAppError().toUserMessage(ErrorContext.PAYMENT_PAY)
  ```

- Son Güncelleme Tarihi: 19.07.2026

- Alternatifler: **Metinleri `strings.xml`'e taşımak** (ViewModel'e `Context`/`resources` sızar veya UiState'te `@StringRes`+args taşımak gerekir; bağlama duyarlı kod→metin eşlemesi yine bir yerde kodlanacaktı). **Her ekranda `Throwable.toXMessage()` uzantısı** — mevcut durum; aşağıya bakınız.

- Sebep: Değişiklik öncesi **17 ViewModel'de ~150 hard-coded Türkçe metin** vardı; `"İnternet bağlantısı kurulamadı."` 17 kez, `"Oturum bulunamadı. Lütfen tekrar giriş yapın."` 18 kez kopyalanmıştı. Metin tablosunun kaynağı `docs/api/openapi.json` uç yanıtlarıdır; yeni uç eklendiğinde artık **yalnız `ErrorMessages.kt`** güncellenir, ViewModel'lere dokunulmaz ("Minimum Değişiklik İlkesi").

- **Yapı iki dosyadır:**
  | Dosya | Sorumluluk |
  |---|---|
  | `util/AppError.kt` | Tek tip hata modeli (`sealed class AppError`: `Network` / `Api(code)` / `Unknown`) + `Throwable.toAppError()` + `AppError.isUnauthorized` |
  | `util/ErrorMessages.kt` | `AppError` → Türkçe metin (`toUserMessage()`) + ekran bağlamı (`ErrorContext`, 26 değer) + `FormMessages` |

- **Çözüm sırası ÖNEMLİDİR** (`resolveApiMessage`, ilk eşleşen kazanır): (1) bağlama özgü net eşleme → (2) ekranlar arası ortak eşleme (401 = oturum yok) → (3) bağlamın genel yedeği (HTTP kodu metinde korunur). Aynı kod ekrana göre farklı çözülür; ör. **403**: `MAP` → "Araçları görmek için ehliyet onayınız gerekli.", `ACTIVE_RENTAL_FINISH` → "Bu yolculuk size ait değil."

- **Kararlar/sapmalar:**
  - **Genel 5xx dalı EKLENMEDİ** (örnekteki kalıptan sapma): mevcut kodda 5xx zaten her ekranın kendi yedeğine düşüyordu ve `IYZICO_INIT` 503'ün kendine özel metni var. Genel bir 5xx dalı bunu gölgeler ve mevcut UI metinlerini değiştirirdi.
  - **401 ortak eşlemesinin İSTİSNALARI** (`CONTEXTS_WITHOUT_SHARED_AUTH`): `LOGIN`'de 401 hata değil **kayıt akışına yönlendirme sinyalidir**; `IYZICO_SETTLE`'da para çekilmiş olduğundan mesaj oturumu değil tahsilatı anlatmalıdır.
  - **`hasDiscount` bayrağı bağlama dönüştü:** `toPayMessage(hasDiscount)` yerine iki ayrı bağlam — `PAYMENT_PAY` / `PAYMENT_PAY_DISCOUNT`.
  - **Yerel form metinleri de taşındı** (`FormMessages`): kayıt doğrulaması, `RegisterError` karşılıkları, "Lütfen bir kart seçin.", `IYZICO_NOT_COMPLETED`, selfie çekim hatası. ViewModel yalnız **hangi ALANA** yazılacağına karar verir.
  - **`retrofit2`/`java.io` artık `ui/` katmanında YOK** (grep ile doğrulandı): akış kontrolü için kalan iki 401 kontrolü (`LoginViewModel`, `SplashViewModel`) `AppError.isUnauthorized`'a çevrildi. Kütüphane sınırı `util/AppError.kt`'de emilir.
  - **Tek metin değişikliği — `AiRecommendationViewModel`:** eski `"Öneri alınamadı: ${e.message}"` ham exception metnini kullanıcıya sızdırıyordu; artık diğer ekranlarla aynı kalıba (`AI_RECOMMENDATION` bağlamı) çözülür. Diğer TÜM metinler birebir korundu (§2.2 — yeni metin uydurulmadı).
  - **`ReservationViewModel.toRentMessage`** ölü koddu; içeriği `RESERVATION_RENT` bağlamına taşındı ve `startDailyRental` oraya bağlandı.

- **Yeni bağımlılık YOK.** UiState/Intent/Screen katmanları **değişmedi** (mesajlar `String` olarak kalır → UI dokunulmadı).

- **Paket sınırı — `data/util` → `data/image` yeniden adlandırıldı (19.07.2026):** Yeni kök `util/` paketi eklenince ağaçta iki "util" belirdi ve kardeş gibi göründüler; oysa değiller. `data/image` (`ImageCompressor` + `File.toImagePart`) `okhttp3`/`Bitmap`'e bağlıdır, `internal`'dır ve YALNIZ iki repository tarafından kullanılır — data katmanından çıkmaz. Kök `util/` ise bağımlılıksızdır ve 15 ViewModel tarafından tüketilir. İkisini birleştirmek OkHttp/Bitmap bağımlılığını ui'ın serbestçe import ettiği bir pakete sızdırırdı. "util" jenerik bir çöp-kutusu adı olduğundan görev adı (`image`) tercih edildi: paket kendi kendini açıklar, sınırı anlatan ek bir nota gerek kalmaz. `git mv` ile taşındığından dosya geçmişi korundu.

- **Dokunulan/eklenen dosyalar:** yeni `util/AppError`, `util/ErrorMessages`; güncellenen 15 ViewModel — `ui/{login,otp,register,profile,map(Map+AiRecommendation),vehicledetail,reservation,rentals,rentalphotos,activerental,selfie,payment,wallet,splash}`.

- Not (19.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.** Emülatör/cihaz doğrulaması yapılmadı.

---

### MVI Kalıp Denetimi ve Hizalama (AGENTS §4)

- Seçim: **`ui/` katmanı AGENTS §4 referans kalıbına birebir hizalandı.** Denetimde bulunan iki ihlal giderildi, iki bilinçli sapma bu kayıtla resmîleştirildi.

- Son Güncelleme Tarihi: 19.07.2026

- **Denetim sonucu (hizalama ÖNCESİ):** §4.4 çekirdek mekaniği 18/18 ViewModel'de eksiksizdi (`@HiltViewModel` + `private _uiState` + `asStateFlow()` + `onIntent`), Contract yapıları (§4.2/§4.3) kurala uygundu ve **hiçbir yerde Effect kanalı (`Channel`/`SharedFlow`) yoktu** — navigasyon state bayrağı + `LaunchedEffect` ile (§4.6). İki ihlal vardı:

- **İhlal 1 — §4.4 "Tek giriş noktası `onIntent`" (9 yer, 8 ekran):** ViewModel'lerde `onIntent` DIŞINDA public fonksiyonlar vardı: `onCodeSentHandled`, `onNavigateToRegisterHandled`, `onVerifiedHandled`, `onRegisteredHandled`, `onProceedHandled`, `onStartedHandled`, `onReservedHandled`, `onRentalStartedHandled`, `onDestinationHandled`. Hepsi aynı işi yapıyordu: ekranın navigasyon bayrağını tükettiğini bildirmek.
  - **Düzeltme:** her biri bir Intent'e çevrildi (`LoginIntent.CodeSentHandled`, `SplashIntent.DestinationHandled` vb.); reducer `onIntent`'in `when`'ine taşındı, ekran `viewModel.onIntent(...)` çağırıyor. **Davranış değişmedi**, yalnız kanal tekleşti.

- **İhlal 2 — §4.5 "stateless gövde yalnız `uiState` + `onIntent` alır":** `MapScreen`'in stateless gövdesi `onNavigateToReservation` parametresi alıyor ve içeriden doğrudan çağırıyordu.
  - **Düzeltme:** yeni `MapIntent.ReserveClicked(vehicleId)`; navigasyonu stateful sarmalayıcı yakalar (`VehicleDismissed` + `onNavigateToReservation`). Parametre stateless imzadan kaldırıldı.
  - **Karşı örnek (İHLAL DEĞİL):** `LicenseScreen`'in `onCaptureFront`/`onCaptureBack` ve `SelfieScreen`'in izin/çekim callback'leri §4.5'in **Android-launcher istisnası** kapsamındadır — durum değiştirmez, yalnız launcher tetikler; sonuç Intent ile döner.

- **Bilinçli sapma 1 — §4.1 üçlüsü olmayan 2 ekran:** `ui/home/HomeScreen.kt` ve `ui/licensepending/LicensePendingScreen.kt` için `Contract`+`ViewModel` **EKLENMEDİ**. İkisi de gerçekten durumsuzdur: Home saf sekme kabıdır (iç `NavHost` + `RencarBottomBar`; her sekme kendi MVI ekranına bağlıdır), LicensePending statik bilgi ekranıdır. Boş bir `UiState()` + boş `onIntent` üretmek kalıbı tatmin eder ama hiçbir şey ifade etmez. **Bu ekranlara durum girdiği anda üçlü kurulacaktır.**

- **Bilinçli sapma 2 — §4.1 `AiRecommendation` yerleşimi:** `ui/map/AiRecommendation{Contract,ViewModel,Dialog}.kt` kendi `ui/<feature>/` paketinde değildir ve `Screen` yerine `Dialog`'dur. Bağımsız bir ekran değil, **Harita'nın alt bileşenidir** (kendi rotası yoktur; `MapScreen` içinden `showAiDialog` ile açılır). Contract+ViewModel ayrımı korunduğundan MVI çekirdeği bozulmaz; yalnız dosya adı/paket kalıbı Map'e tabidir.

- **Yeni bağımlılık YOK.** UiState alanları, Screen tasarımları ve navigasyon davranışı **değişmedi**.

- **Dokunulan dosyalar:** `ui/login/*`, `ui/otp/*`, `ui/register/*`, `ui/license/*`, `ui/splash/*`, `ui/rentalphotos/*`, `ui/reservation/*` (her biri Contract+ViewModel+Screen), `ui/map/{MapContract,MapViewModel,MapScreen}`.

- Not (19.07.2026): **KOD HİZALANDI — `:app:assembleDebug` başarılı.** Denetim grep ile tekrarlandı: `onIntent` dışında public VM fonksiyonu YOK, stateless gövdelerde navigasyon callback'i YOK. Emülatör/cihaz doğrulaması yapılmadı.

---

### Foto Akışı Devralma + Rezervasyon Geri Sayımı/Kurtarma — `GET /rentals/{id}/photos`, `GET /reservations/active`

- Seçim: **Bağlanmamış iki müşteri ucu bağlandı ve tükettikleri UI/akış eklendi.** (1) **`GET /rentals/{id}/photos`** — foto ekranı açılışında kullanıcının açık PREPARING kiralaması varsa yeni açmak yerine akış DEVRALINIR (yüklü yönler/sayaç geri yüklenir). (2) **`GET /reservations/active`** — rezervasyon ekranında 15 dk ücretsiz tutmanın kalanı geri sayımla gösterilir; ayrıca yeniden açılışta CUSTOMER'ın devam eden akışı (ACTIVE kiralama / PREPARING foto / aktif rezervasyon) Splash'te kurtarılır.

- Son Güncelleme Tarihi: 20.07.2026

- Sebep: `docs/api/openapi.json`'da bu iki müşteri ucunun istemcisi yoktu (denetimle bulundu). Foto devralma openapi resume sözleşmesinin ("uygulama yeniden açıldığında yarım kalan akış buradan devralınır") karşılığıdır; rezervasyon geri sayımı 15 dk ücretsiz tutmanın kalanını gösterir. Yeniden açılış kurtarması, mevcut `SPLASH` oturum-geri-yükleme kalıbının doğal uzantısıdır (aynı "çöz → popUpTo(SPLASH, inclusive) ile geç" deseni).

- **Kritik kontrat notu (§2.2 — kullanıcıyla netleştirildi):** 15 dk sayaç **rezervasyona** aittir, kiralamaya değil. `POST /rentals` foto ekranına girer girmez rezervasyonu **CONVERTED** yapar (araç RESERVED→RENTED, kiralama PREPARING); dolayısıyla foto aşamasında `GET /reservations/active` **404** döner ve PREPARING'in sunucu-tarafı sayacı yoktur. Bu yüzden "foto sırasında sayacı sürdür" yerine sayaç yalnız **rezervasyon aşamasında** gösterilir. İlk yanlış-anlaşılan "foto sırasında 15 dk aksın" isteği bu contract ışığında kullanıcı tarafından geri çekildi.

- **Kararlar/sapmalar:**
  - **DTO izolasyonu korunur ("Katman Derinliği"):** yeni `ReservationUi` + `ResumableRentalUi` (data/model), dönüşümler mapper katmanında (`ReservationResponse.toUi`, `RentalResponse.toResumableUi`); `ui/` içinde `data.remote.dto` importu yoktur.
  - **Kurtarma — araç görünürlük kısıtı:** `GET /vehicles/{id}` REZERVE aracı sahibine bile **404** döndürür (yalnız AVAILABLE veya aktif KİRALAMASI olana görünür — openapi). Bu yüzden yeniden açılışta rezervasyon ekranı aracı yükleyemez; `ReservationViewModel` bunu **hata değil kurtarma** sayar ve `GET /reservations/active`'in araç özetinden **minimal kurtarma görünümü** (geri sayım + "Devam Et") gösterir. Tam araç kartı/plan seçimi yalnız araç yüklenebildiğinde çıkar; minimal görünümde plan varsayılan (Dakikalık) kalır.
  - **409 retry düzeltmesi:** aktif rezervasyon varken alt buton "Devam Et" olur ve `POST /reservations` **atılmaz** (aksi halde "zaten aktif rezervasyonun var" 409'u gelirdi — bkz. "Günlük Kiralama" kararındaki askıda-rezervasyon notu); doğrudan foto akışına/DAILY kiralamaya geçilir.
  - **Geri sayım = yerel ticker:** `remainingSeconds` sunucu gerçeği; ekran 1 sn'lik yerel sayaçla azaltır (Aktif Yolculuk resync kalıbıyla aynı), 0'da gizlenir. Foto aşamasında poll YOKTUR (uç 404).
  - **Foto devralma yalnız süreç-ölümü içindir:** ekrandan bilerek **geri** çıkış hâlâ PREPARING'i iptal eder (`onCleared`); yalnız beklenmedik kapanışta (onCleared çalışmaz) kiralama sunucuda kalır ve açılışta devralınır. Normal rezervasyon→foto akışında henüz PREPARING olmadığından `findPreparingRental` null döner → eski davranış (yeni `POST /rentals`).
  - **`SplashDestination` enum → sealed interface:** kurtarma hedefleri kimlik taşıdığından (rentalId / vehicleId+plan) sealed'e çevrildi; NavHost route'u bu alanlardan üretir. Kurtarma **Splash'e özeldir**, `OtpVerificationViewModel.resolveDestination`'a replike EDİLMEDİ (giriş sonrası kullanıcı zaten ilgili ekrandan gelir). PENDING/ehliyet kuralı iki VM'de aynı kalır.
  - **Kurtarma sırası:** ACTIVE kiralama → Aktif Yolculuk; PREPARING → Foto (devralma); aktif rezervasyon → Rezervasyon (geri sayım). Tek `listMine` çağrısıyla ACTIVE/PREPARING değerlendirilir; ağ/404'te sessizce Home'a düşülür (açılış bloklanmaz).
  - **Rezervasyon iptali (`DELETE /reservations/{id}`) KAPSAM DIŞI** — kullanıcı "sonra" dedi; minimal kurtarma görünümünde yalnız "Devam Et" var, "İptal" ayrı iş.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli; DI değişmedi (mevcut arayüzlere metot eklendi).

- **Dokunulan/eklenen dosyalar:** yeni `data/model/ReservationUi`; güncellenen `data/remote/api/{ReservationApi,RentalApi}` (getActive/getPhotos), `data/mapper/{ReservationMapper,RentalMapper}` (toUi/toResumableUi), `data/model/RentalUi` (ResumableRentalUi), `data/repository/{ReservationRepository,RentalRepository}` (getActiveReservation/getPhotos/findPreparingRental/findResumableRental), `ui/rentalphotos/RentalPhotosViewModel` (resumeOrCreate), `ui/reservation/{ReservationContract,ReservationViewModel,ReservationScreen}` (geri sayım + kurtarma görünümü + "Devam Et"), `ui/splash/{SplashContract,SplashViewModel}` (sealed hedef + kurtarma), `ui/navigation/RencarNavHost` (kurtarma route eşlemesi).

- Not (20.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` + `:app:assembleDebug` başarılı.** Emülatör/cihaz doğrulaması yapılmadı (kurtarma senaryoları CUSTOMER hesabı + aktif rezervasyon/kiralama state'i gerektirir).

---

### Rezervasyon → Foto → Başlat: Kiralama Oluşturmayı Erteleme + Foto Ekranından İptal — `DELETE /reservations/{id}`

- Seçim: **Foto ekranında `POST /rentals` artık AÇILIŞTA çağrılmaz; yalnız "Kiralamayı Başlat" anında çalışır.** Böylece rezervasyon (15 dk ücretsiz tutma) "Başlat"a dek AKTİF kalır, **geri sayım foto ekranında gösterilir** ve foto ekranına görünür bir **"Rezervasyonu İptal Et"** butonu (`DELETE /reservations/{id}`) eklenir. Çekilen 4 kare **yerelde** tutulur; "Başlat" tek zincirle `POST /rentals` (rezervasyon CONVERTED) → 4× `POST /rentals/{id}/photos` → `POST /rentals/{id}/start` çalıştırır. **Rezervasyon yalnız bu butonla iptal edilir**; ekrandan geri çıkış iptal ETMEZ (`onCleared` iptali kaldırıldı).

- Son Güncelleme Tarihi: 20.07.2026

- **BU KARAR, aynı gün alınan "Foto Akışı Devralma + Rezervasyon Geri Sayımı/Kurtarma" kararındaki üç notu GEÇERSİZ KILAR** (kullanıcı isteğini netleştirdi):
  1. *"foto sırasında 15 dk aksın isteği kullanıcı tarafından geri çekildi"* → **geri alındı**: kullanıcı geri sayımın foto ekranında görünmesini açıkça istedi. Bunu mümkün kılan tek yol `POST /rentals`'i ertelemek olduğundan (aksi halde rezervasyon CONVERTED olur, `GET /reservations/active` 404 döner, sayaç kaybolur) kiralama oluşturma "Başlat"a taşındı.
  2. *"`DELETE /reservations/{id}` KAPSAM DIŞI"* → **artık kapsamda**: uç bağlandı ve foto ekranındaki iptal butonuna verildi.
  3. *"ekrandan bilerek geri çıkış PREPARING'i `onCleared` ile iptal eder"* → **kaldırıldı**: foto aşamasında henüz PREPARING yoktur (ertelendi); iptal yalnız butonla yapılır, geri çıkış rezervasyonu 15 dk TTL'ine bırakır.

- Sebep: Kullanıcının tarif ettiği akış ("Tamamla → 15 dk başlar → sayaç foto ekranında → 'Başlat'a dek rezervasyon tamamlanmış sayılmaz → iptal sadece burada") API sözleşmesiyle ancak kiralama oluşturmayı "Başlat"a erteleyerek tutarlı olur. Fotoğraf yalnız bir kiralamaya yüklenebildiğinden (`POST /rentals/{id}/photos`) ve kiralama oluşturmak rezervasyonu CONVERTED yaptığından, kareler yerelde tutulup toplu olarak "Başlat"ta yüklenir — rezervasyonu foto boyunca aktif tutmanın başka yolu yoktur (§2.2).

- **Kararlar/sapmalar:**
  - **İki mod (init):** önce `findPreparingRental()` — askıda PREPARING kiralama varsa (yarım "Başlat" ya da süreç ölümü; rezervasyon o noktada CONVERTED) **kurtarma modu** (foto durumu `GET /rentals/{id}/photos`'tan, geri sayım yok, iptal `DELETE /rentals/{id}`); yoksa `GET /reservations/active` ile **normal mod** (geri sayım + yerel çekim + iptal `DELETE /reservations/{id}`). Kurtarma önce denenir ki dangling PREPARING kullanıcıyı yeni rezervasyondan kilitlemesin.
  - **"Başlat" resume-güvenli:** zincir ortasında hata olursa `rentalId` + yüklenmiş yönler state'te korunur; kullanıcı tekrar basınca create atlanır, yalnız eksik yönler yüklenir, start çağrılır (yeniden 409 riski yok).
  - **İptal tek buton, iki uç:** kiralama henüz yoksa `DELETE /reservations/{id}` (aktif olduğundan çalışır; 403/404/409 → `ErrorContext.RESERVATION_CANCEL` metni), kiralama oluştuysa `DELETE /rentals/{id}` (mevcut sessiz temizlik). İkisi de aracı anında AVAILABLE yapar; başarıda Home'a dönülür (Ödeme başarısı kalıbı).
  - **Süre dolması:** normal modda yerel sayaç 0'a inince araç sunucuda boşa çıktığından "Rezervasyon süresi doldu" bilgilendirmesi + "Ana sayfaya dön" gösterilir (kullanıcı onayıyla; sessiz değil).
  - **Kurtarma sırası (Splash) değişmedi:** foto aşamasında yalnız rezervasyon olduğundan süreç ölümünde `resolveActiveFlow` → `ActiveReservation` → Rezervasyon ekranı ("Devam Et" → foto ekranı). Yerel kareler kaybolur, yeniden çekilir. "Başlat" bir PREPARING oluşturduysa süreç ölümünde `PreparingRental` → foto ekranı kurtarma modu.
  - **DTO izolasyonu korunur ("Katman Derinliği"):** `ReservationApi.cancel` `Response<Unit>` döner; 204 dışı yanıt `HttpException`'a çevrilir (PaymentRepository kart-silme kalıbı) ki 4xx `Result.failure` olsun. `ui/` içinde `data.remote.dto` importu yoktur.

- **Yeni bağımlılık YOK.** Mevcut Retrofit + kotlinx.serialization + Hilt + Compose yeterli; DI değişmedi (mevcut `ReservationApi`'ye metot eklendi).

- **Dokunulan/eklenen dosyalar:** güncellenen `data/remote/api/ReservationApi` (cancel), `data/repository/ReservationRepository` (cancelReservation), `util/ErrorMessages` (RESERVATION_CANCEL), `ui/rentalphotos/{RentalPhotosContract,RentalPhotosViewModel,RentalPhotosScreen}` (iki mod + yerel çekim + geri sayım + Başlat zinciri + iptal), `ui/navigation/{RencarNavHost,RencarDestinations}` (onCancelled + yorum). `RentalPhotosViewModel`'den `appScope` (yalnız eski `onCleared` iptali için vardı) kaldırıldı.

- Not (20.07.2026): **KOD HİZALANDI — `:app:compileDebugKotlin` başarılı.** Emülatör/cihaz doğrulaması yapılmadı (CUSTOMER hesabı + aktif rezervasyon state'i gerektirir).
