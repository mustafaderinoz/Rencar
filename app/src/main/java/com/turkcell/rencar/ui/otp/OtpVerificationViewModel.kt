package com.turkcell.rencar.ui.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.turkcell.rencar.ui.navigation.RencarDestinations
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // Login → OTP geçişinde iletilen numara (path argümanı). Ülke kodu sabit +90
    // olduğundan gösterim için "+90 " ile ön eklenir (bkz. LoginUiState).
    private val phoneNumber: String =
        savedStateHandle.get<String>(RencarDestinations.OTP_ARG_PHONE)
            ?.let { "+90 $it" }
            .orEmpty()

    private val _uiState = MutableStateFlow(OtpVerificationUiState(phoneNumber = phoneNumber))
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
