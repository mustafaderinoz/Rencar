# RenCar - ViewModel ve UI Bağlama Kuralları

> ViewModel, UI bağlama (Route/Screen) ve DI için **tek doğruluk kaynağıdır**.
> Sözleşme için bkz. [mvi-contracts.md](mvi-contracts.md), genel akış için [mvi-overview.md](mvi-overview.md).
>
> Referans: `ui/auth/login/LoginViewModel.kt`, `LoginScreen.kt`, `di/AuthModule.kt`.
> (RenCar girişi telefon numarası + SMS/OTP tabanlıdır: "Telefon numaranı gir → Kod Gönder".)

---

## 1. Temel Kural

> ViewModel'in UI ile tek temas noktası `fun onIntent(intent: <Screen>Intent)`'tir.
> State `StateFlow`, Effect `Channel` ile dışarı açılır; her ikisinin de mutable hali
> **private** tutulur. ViewModel içinde **hiçbir Android/Compose/Context bağımlılığı bulunamaz.**

---

## 2. ViewModel İskeleti

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = Channel<LoginEffect>(Channel.BUFFERED)
    val effect: Flow<LoginEffect> = _effect.receiveAsFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.PhoneNumberChanged -> updateForm { it.copy(phoneNumber = intent.value) }
            is LoginIntent.CountryCodeChanged -> updateForm { it.copy(countryCode = intent.value) }
            is LoginIntent.Submit             -> submit()
        }
    }
}
```

---

## 3. Zorunlu Kurallar

1. `@HiltViewModel` + `@Inject constructor`. Bağımlılıklar `private val` olarak alınır.
2. `MutableStateFlow` private; dışarı `asStateFlow()` ile `StateFlow` olarak açılır.
3. Tek seferlik olaylar `Channel(Channel.BUFFERED)` private; dışarı `receiveAsFlow()` ile açılır.
4. Tek giriş noktası `onIntent(...)`; `when` **tüm** Intent dallarını kapsar (exhaustive).
   Dallar `is ...` biçiminde yazılır.
5. Asenkron iş `viewModelScope.launch { ... }` içinde, repository üzerinden yapılır.
6. **Yasak:** ViewModel içinde `Context`, `View`, `@Composable`, `Activity`, navigasyon API'si.
7. State güncellemeleri yalnızca `_uiState.update { it.copy(...) }` ile yapılır (atomik).

---

## 4. Türetilen Alanlar

Buton aktifliği ("Kod Gönder") gibi türetilen alanlar, ilgili alan her değiştiğinde
yeniden hesaplanır. Türetme tek bir yardımcıda toplanır:

```kotlin
private fun updateForm(transform: (LoginUiState) -> LoginUiState) {
    _uiState.update { current ->
        val updated = transform(current)
        updated.copy(isSubmitEnabled = updated.isFormValid())
    }
}

private fun LoginUiState.isFormValid(): Boolean =
    countryCode.isNotBlank() && phoneNumber.filter { it.isDigit() }.length == 10
```

---

## 5. Asenkron Akış ve Effect

- İşlem başlarken/biterken `isLoading` güncellenir; çift gönderim için erken `return` ile korunur.
- Sonuç `Result<…>` üzerinden ele alınır; başarı/hata Effect'e dönüştürülür.
- Başarıda OTP ekranına gidilir (`NavigateToOtp`), numara Effect ile taşınır.

```kotlin
private fun submit() {
    val state = _uiState.value
    if (!state.isSubmitEnabled || state.isLoading) return
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val fullNumber = state.countryCode + state.phoneNumber.filter { it.isDigit() }
        val result = authRepository.sendVerificationCode(fullNumber)
        _uiState.update { it.copy(isLoading = false) }
        result
            .onSuccess { _effect.send(LoginEffect.NavigateToOtp(fullNumber)) }
            .onFailure { _effect.send(LoginEffect.ShowError(it.message ?: "Kod gönderilemedi.")) }
    }
}
```

---

## 6. UI Bağlama: Route + Screen

İki composable zorunludur:

- **`<Screen>Route`** (durumlu/stateful): ViewModel'i `hiltViewModel()` ile alır, state'i
  `collectAsStateWithLifecycle()` ile toplar, Effect'leri `LaunchedEffect` içinde tüketir.
  Tek MVI köprüsü burasıdır.
- **`<Screen>Screen`** (durumsuz/stateless): `state: <Screen>UiState` ve
  `onIntent: (<Screen>Intent) -> Unit` parametrelerini alır; preview edilebilir olmalıdır.

```kotlin
@Composable
fun LoginRoute(
    onNavigateToOtp: (phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginEffect.ShowError     -> snackbarHostState.showSnackbar(effect.message)
                is LoginEffect.NavigateToOtp -> onNavigateToOtp(effect.phoneNumber)
            }
        }
    }
    LoginScreen(
        state = uiState,
        onIntent = viewModel::onIntent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
```

**Yasak:** `<Screen>Screen` içinde ViewModel referansı, repository çağrısı veya iş mantığı.
UI yalnızca `onIntent(...)` ile niyet yayar. Navigasyon Route'a dışarıdan verilen
lambda (`onNavigateToOtp`) ile yapılır; ViewModel navigasyonu bilmez.

---

## 7. DI Modülü (Repository Bağlama)

Interface → implementasyon bağlaması `di/<X>Module.kt` içinde `@Binds` ile yapılır:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FakeAuthRepository): AuthRepository
}
```

- Implementasyon sınıfı `@Inject constructor` ile inject edilebilir olmalıdır.
- Gerçek API (SMS sağlayıcısı) geldiğinde yalnızca `@Binds` hedefi değiştirilir.

---

## 8. Sözleşme Örneği (bu ekran için)

Kurala uyması gereken Intent/State/Effect üçlüsü — referans olması için:

```kotlin
sealed interface LoginIntent {
    data class PhoneNumberChanged(val value: String) : LoginIntent
    data class CountryCodeChanged(val value: String) : LoginIntent
    data object Submit : LoginIntent
}

data class LoginUiState(
    val countryCode: String = "+90",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val isSubmitEnabled: Boolean = false,
)

sealed interface LoginEffect {
    data class NavigateToOtp(val phoneNumber: String) : LoginEffect
    data class ShowError(val message: String) : LoginEffect
}
```

> Not: OTP ekranı (`ui/auth/otp/OtpViewModel.kt`) aynı kurallarla kurulur —
> Intent: `CodeChanged`, `Verify`, `ResendCode`; Effect: `NavigateToHome`,
> `ShowError`; türetilen alan: 6 hane girildiğinde `isVerifyEnabled`.