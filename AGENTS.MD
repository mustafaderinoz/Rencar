## RenCar -  Araç Kiralam Uygulaması
### Bu repository RenCar adında bir araç kiralama uygulamasının kaynak kodlarını içermektedir. Bu agents.md dosyası bu projede çalışan insan/ai herkesin uyması ZORUNDA olduğu genel kuralları içerir.
---

## 1) TEKNOLOJİ YIĞINI

- Mobil: Android Jetpack Compose KOTLIN

- Backend: Diğer takım tarafından hazırlanmış bir Restful API.

## 2) GENEL ÇALIŞMA PRENSİPLERİ

### 2.1) TEK SEFERDE DOSYA LİMİTİ

Hangi işlem olursa olsun tek seferde maksimum (birbiriyle alakalı) 5 dosyalar halinde çalışmak zorundasın. İşlemi birbiriyle bağlantılı batchlere bölmek zorundasın. Eğer bunun aksi talep edilirse DUR ve EK ONAY iste.

### 2.2) UYDURMAK YASAK (NO INVENTING)

Eğer herhangi bir operasyonda bilgi ya da referans eksikliği/hatası yaşıyorsan buradaki eksik/hatalı bilgiyi uydurmak yasak. Böyle bir durumda operasyonu durdur ve kullanıcıya sorarak ilerle.

### 2.3) ÖNCE PLANLA, SONRA KODLA

Kod üretmeden önce şunları yapmak zorundasın;

- Bir dosya dökümü hazırla (hangi dosyalar değişecek/eklenecek/silinecek + neden)

- Eğer varsa yeni bağımlılıklar matrisi (Hangi kütüphane, versiyon + neden)

- Planı sun, onay almadan asla implementasyona başlama.

## 3) ÇIKTI FORMATI

Her implementasyon ya da plan sonrası aşağıdaki çıktı formatına uymak zorundasın.

## 4) MVI REFERANS YAPISI (ZORUNLU)

Yeni bir MVI ekranı/özelliği üretilirken aşağıdaki referans kalıba **birebir** uyulmak zorundadır. Mevcut referans implementasyonu `ui/login/*` dosyalarıdır; bu yapı bozulamaz, yeni işler aynı kalıbı izler.

### 4.1) Paket ve dosya düzeni

Her özellik `ui/<feature>/` paketi altında, üç dosya halinde:

- `<Feature>Contract.kt` → `<Feature>UiState` (state) + `<Feature>Intent` (intent)
- `<Feature>ViewModel.kt` → `@HiltViewModel` ViewModel
- `<Feature>Screen.kt` → stateful + stateless composable çifti

### 4.2) State

- Tek bir `data class <Feature>UiState` ile temsil edilir.
- Tüm alanlar varsayılan değerlere sahiptir (`= ""`, `= false` vb.).
- Yalnızca saf UI durumunu tutar.

### 4.3) Intent

- `sealed interface <Feature>Intent` ile tanımlanır.
- Parametreli aksiyonlar `data class`, parametresizler `data object` olur.

### 4.4) ViewModel

- `@HiltViewModel` + `@Inject constructor(...)` ile işaretlenir.
- `private val _uiState = MutableStateFlow(<Feature>UiState())` tutar; dışarıya `val uiState: StateFlow<...> = _uiState.asStateFlow()` ile açılır.
- Tek giriş noktası `fun onIntent(intent: <Feature>Intent)`; reducer mantığı bir `when` içinde `_uiState.update { ... }` ile çalışır.

### 4.5) Ekran (Compose)

- **Stateful sarmalayıcı**: `viewModel: <Feature>ViewModel = hiltViewModel()` alır, `collectAsStateWithLifecycle()` ile state'i toplar ve stateless gövdeyi besler.
- **Stateless gövde**: yalnızca `uiState` + `onIntent: (<Feature>Intent) -> Unit` parametreleriyle çizer. Geri dâhil tüm navigasyon ve durum değiştiren aksiyonlar `onIntent` üzerinden gider (ör. geri için `BackClicked` intent'i; navigasyonu sarmalayıcı yakalar).
  - **İstisna (Android-API tetikleyicileri):** bir Android launcher'ını BAŞLATAN callback'ler — kamera çekimi (`onCapture`), izin isteme (`onGrantPermission`) gibi — ek parametre olarak geçirilebilir. Bunlar durumu değiştirmez; yalnız launcher'ı tetikler ve SONUÇ bir intent'le (`PhotoCaptured`, `PermissionResult` vb.) VM'e döner. Alt composable'ların kendi özel callback'leri (leaf) bu kuralın kapsamı dışındadır; kural üst stateless gövdeye uygulanır.
- `@Preview`'lar stateless gövdeyi Hilt'siz, sabit `<Feature>UiState()` ile render eder.

### 4.6) Katman sınırı

- `<Feature>Effect` (tek seferlik olay), navigasyon, usecase, repository katmanları **varsayılan olarak EKLENMEZ**.
- Bunlar yalnızca açıkça talep edilirse eklenir (§2.2 uydurmak yasak kuralıyla uyumlu).