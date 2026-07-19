package com.turkcell.rencar.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.isUnauthorized
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.PhoneChanged ->
                // Yalnızca rakam; +90 sonrası 10 hane (5XX XXX XX XX). Yeni girişte hatayı temizle.
                _uiState.update {
                    it.copy(
                        phone = intent.phone.filter(Char::isDigit).take(10),
                        errorMessage = null,
                    )
                }

            LoginIntent.SendCodeClicked -> sendCode()

            // Navigasyon ekran (Screen) katmanında ele alınır.
            LoginIntent.BackClicked -> Unit
            LoginIntent.RegisterClicked -> Unit

            // Ekran geçişi yaptı → bayrakları tüket (tekrar geçişi önler).
            LoginIntent.CodeSentHandled -> _uiState.update { it.copy(codeSent = false) }
            LoginIntent.NavigateToRegisterHandled ->
                _uiState.update { it.copy(navigateToRegister = false) }
        }
    }

    /** POST /auth/login: telefonu E.164'e çevirip kod gönderimini tetikler. */
    private fun sendCode() {
        val state = _uiState.value
        if (state.phone.length != 10 || state.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.login(phone = "+90${state.phone}")
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, codeSent = true) }
                }
                .onFailure { e ->
                    // 401 = "bu numaraya kayıtlı kullanıcı yok". Backend kayıtsız numaraya OTP
                    // göndermediğinden kayıtsızlık ancak burada anlaşılır: hata göstermek yerine
                    // kullanıcı kayıt ekranına alınır (numara oraya taşınır).
                    if (e.toAppError().isUnauthorized) {
                        _uiState.update { it.copy(isLoading = false, navigateToRegister = true) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = e.toAppError().toUserMessage(ErrorContext.LOGIN),
                            )
                        }
                    }
                }
        }
    }
}
