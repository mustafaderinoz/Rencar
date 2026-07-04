package com.turkcell.rencar.ui.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.PhoneChanged ->
                // Yalnızca rakam; +90 sonrası 10 hane (5XX XXX XX XX).
                _uiState.update {
                    it.copy(phone = intent.phone.filter(Char::isDigit).take(10))
                }

            // §4.6: navigasyon/effect/API katmanı varsayılan olarak eklenmez; yalnızca state.
            LoginIntent.SendCodeClicked -> Unit
            LoginIntent.BackClicked -> Unit
            LoginIntent.RegisterClicked -> Unit
        }
    }
}
