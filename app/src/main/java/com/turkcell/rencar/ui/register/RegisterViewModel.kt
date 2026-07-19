package com.turkcell.rencar.ui.register

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.RegisterError
import com.turkcell.rencar.data.model.RegisterException
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.FormMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Login → Register geçişinde iletilen numara (10 hane). "Kayıt ol" linkinden gelindiğinde boş.
    private val prefilledPhone: String =
        savedStateHandle.get<String>(RencarDestinations.REGISTER_ARG_PHONE).orEmpty()

    private val _uiState = MutableStateFlow(RegisterUiState(phone = prefilledPhone))
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            // Her düzenlemede ilgili alanın hatası ve genel form hatası temizlenir.
            is RegisterIntent.FullNameChanged ->
                _uiState.update {
                    it.copy(fullName = intent.fullName, fullNameError = null, formError = null)
                }

            is RegisterIntent.EmailChanged ->
                _uiState.update {
                    it.copy(email = intent.email.trim(), emailError = null, formError = null)
                }

            is RegisterIntent.PasswordChanged ->
                _uiState.update {
                    it.copy(password = intent.password, passwordError = null, formError = null)
                }

            is RegisterIntent.PhoneChanged ->
                // Login ile aynı kural: yalnızca rakam, +90 sonrası 10 hane.
                _uiState.update {
                    it.copy(
                        phone = intent.phone.filter(Char::isDigit).take(PHONE_DIGIT_COUNT),
                        phoneError = null,
                        formError = null,
                    )
                }

            is RegisterIntent.ReferralCodeChanged ->
                _uiState.update {
                    it.copy(
                        referralCode = intent.referralCode.trim(),
                        referralCodeError = null,
                        formError = null,
                    )
                }

            RegisterIntent.SubmitClicked -> submit()

            // Navigasyon ekran (Screen) katmanında ele alınır.
            RegisterIntent.BackClicked -> Unit
            RegisterIntent.LoginClicked -> Unit

            // Ekran geçişi yaptı → bayrağı tüket (tekrar geçişi önler).
            RegisterIntent.RegisteredHandled -> _uiState.update { it.copy(registered = false) }
        }
    }

    /**
     * POST /auth/register: önce yerel alan doğrulaması (kullanıcıyı gereksiz ağ turundan kurtarır;
     * sunucu aynı kuralları zaten uygular), sonra kayıt. Başarıda token DÖNSE DE kaydedilmez —
     * repository yok sayar ve kullanıcı Login'e döner (bkz. AuthRepository.register).
     */
    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return

        // Her denemede alan hataları baştan hesaplanır: aksi halde önceki denemeden kalan sunucu
        // hatası (ör. "Bu e-posta zaten kayıtlı") istek uçarken ekranda asılı kalırdı.
        _uiState.update { it.validate() }
        if (_uiState.value.hasFieldError) return

        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            authRepository.register(
                fullName = state.fullName.trim(),
                email = state.email.trim(),
                password = state.password,
                phone = "+90${state.phone}",
                referralCode = state.referralCode,
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, registered = true) }
                }
                .onFailure { e ->
                    val error = (e as? RegisterException)?.error ?: RegisterError.Unknown
                    _uiState.update { error.applyTo(it.copy(isLoading = false)) }
                }
        }
    }

    /** Zorunlu alanları yerel olarak doğrular; davet kodu isteğe bağlı olduğundan kontrol edilmez. */
    private fun RegisterUiState.validate(): RegisterUiState = copy(
        fullNameError = FormMessages.FULL_NAME_BLANK.takeIf { fullName.isBlank() },
        emailError = FormMessages.INVALID_EMAIL.takeIf {
            !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        },
        passwordError = FormMessages.passwordTooShort(PASSWORD_MIN_LENGTH).takeIf {
            password.length < PASSWORD_MIN_LENGTH
        },
        phoneError = FormMessages.INVALID_PHONE.takeIf { phone.length != PHONE_DIGIT_COUNT },
    )

    private val RegisterUiState.hasFieldError: Boolean
        get() = fullNameError != null || emailError != null ||
            passwordError != null || phoneError != null

    /**
     * Tiplenmiş kayıt hatasını ilgili ALANA yazar. Çakışan e-posta/telefon AYNI HTTP kodunu (409)
     * döndüğünden ayrım mapper katmanında yapılır; metinler [FormMessages]'ta durur — burada yalnız
     * hangi alanın işaretleneceği kalır.
     */
    private fun RegisterError.applyTo(state: RegisterUiState): RegisterUiState = when (this) {
        RegisterError.EmailTaken ->
            state.copy(emailError = FormMessages.EMAIL_TAKEN)

        RegisterError.PhoneTaken ->
            state.copy(phoneError = FormMessages.PHONE_TAKEN)

        RegisterError.InvalidReferral ->
            state.copy(referralCodeError = FormMessages.INVALID_REFERRAL)

        // Sunucunun kendi (Türkçe, kullanıcıya gösterilebilir) doğrulama metinleri.
        is RegisterError.Validation ->
            state.copy(formError = messages.joinToString("\n"))

        RegisterError.Network ->
            state.copy(formError = FormMessages.REGISTER_NETWORK)

        RegisterError.Unknown ->
            state.copy(formError = FormMessages.REGISTER_UNKNOWN)
    }
}
