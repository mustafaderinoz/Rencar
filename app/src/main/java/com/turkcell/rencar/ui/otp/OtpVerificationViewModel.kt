package com.turkcell.rencar.ui.otp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OtpVerificationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OtpVerificationUiState())
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    fun onIntent(intent: OtpVerificationIntent) {
        when (intent) {
            is OtpVerificationIntent.OtpCodeChanged ->
                // Yalnızca rakam; maksimum 6 hane.
                _uiState.update {
                    it.copy(otpCode = intent.code.filter(Char::isDigit).take(6))
                }

            // §4.6: navigasyon/effect/API katmanı varsayılan olarak eklenmez; yalnızca state.
            OtpVerificationIntent.VerifyClicked -> Unit
            OtpVerificationIntent.ResendCodeClicked -> Unit
            OtpVerificationIntent.BackClicked -> Unit
        }
    }
}
