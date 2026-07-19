package com.turkcell.rencar.ui.otp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.data.repository.LicenseRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OtpVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val licenseRepository: LicenseRepository,
) : ViewModel() {

    // Login → OTP geçişinde iletilen numara (10 haneli, path argümanı).
    private val phoneDigits: String =
        savedStateHandle.get<String>(RencarDestinations.OTP_ARG_PHONE).orEmpty()

    // API için E.164 ("+90XXXXXXXXXX"); gösterim için "+90 XXXXXXXXXX".
    private val e164Phone: String = "+90$phoneDigits"

    private val _uiState = MutableStateFlow(OtpVerificationUiState(phoneNumber = "+90 $phoneDigits"))
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    // Geri sayım işi; her yeniden başlatmada (init/resend) iptal edilip yeniden kurulur.
    private var timerJob: Job? = null

    init {
        startCountdown()
    }

    /** OTP geçerlilik süresini baştan (4:59) başlatıp saniyede bir 0'a kadar azaltır. */
    private fun startCountdown() {
        timerJob?.cancel()
        _uiState.update { it.copy(timeRemaining = OTP_DURATION_SECONDS) }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeRemaining > 0) {
                delay(1000)
                _uiState.update { it.copy(timeRemaining = (it.timeRemaining - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun onIntent(intent: OtpVerificationIntent) {
        when (intent) {
            is OtpVerificationIntent.OtpCodeChanged ->
                // Yalnızca rakam; maksimum 6 hane. Yeni girişte hatayı temizle.
                _uiState.update {
                    it.copy(
                        otpCode = intent.code.filter(Char::isDigit).take(6),
                        errorMessage = null,
                    )
                }

            OtpVerificationIntent.VerifyClicked -> verify()

            // Tekrar kod: geri sayımı 4:59'dan yeniden başlat (yeniden gönderim SMS akışı ileride).
            OtpVerificationIntent.ResendCodeClicked -> startCountdown()

            // Navigasyon ekran katmanında ele alınır.
            OtpVerificationIntent.BackClicked -> Unit

            // Ekran geçişi yaptı → bayrağı tüket (tekrar geçişi önler).
            OtpVerificationIntent.VerifiedHandled -> _uiState.update { it.copy(verified = false) }
        }
    }

    /** POST /auth/verify-otp: kodu doğrular; başarılıysa token saklanır ve geçiş sinyali verilir. */
    private fun verify() {
        val state = _uiState.value
        if (state.otpCode.length != 6 || state.isLoading) return

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.verifyOtp(phone = e164Phone, code = state.otpCode)
                .onSuccess { user ->
                    val destination = resolveDestination(user.role)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            verified = true,
                            destination = destination,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.OTP),
                        )
                    }
                }
        }
    }

    /**
     * OTP doğrulaması sonrası gidilecek hedefi belirler. Onaylı roller (CUSTOMER/ADMIN) doğrudan
     * Home'a gider. PENDING kullanıcıda GET /license/status'a bakılır:
     * - UNDER_REVIEW → [PostVerifyDestination.LICENSE_PENDING] (engelleyici bekleme ekranı)
     * - APPROVED → [PostVerifyDestination.HOME]
     * - NOT_SUBMITTED / REJECTED / durum alınamadı → [PostVerifyDestination.LICENSE_UPLOAD]
     * (Durum alınamazsa güvenli varsayılan: kullanıcı ehliyet yükleyebilsin diye yükleme ekranı.)
     */
    private suspend fun resolveDestination(role: String): PostVerifyDestination {
        if (role != "PENDING") return PostVerifyDestination.HOME
        return when (licenseRepository.getStatus().getOrNull()) {
            LicenseVerificationStatus.UNDER_REVIEW -> PostVerifyDestination.LICENSE_PENDING
            LicenseVerificationStatus.APPROVED -> PostVerifyDestination.HOME
            else -> PostVerifyDestination.LICENSE_UPLOAD
        }
    }

    companion object {
        // OTP 5 dk geçerli; sayaç 4:59'dan başlasın (openapi: kod 5 dakika geçerli).
        private const val OTP_DURATION_SECONDS = 299
    }
}
