package com.turkcell.rencar.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Açılış (session restore) ViewModel'i (§4.4).
 *
 * Amaç: kullanıcı uygulamayı kapatıp açtığında, saklı token'larla oturumu geri yükleyip onu her
 * seferinde Login'e zorlamamak. `SessionManager`/`TokenAuthenticator` altyapısını doğrudan kullanır:
 * açılıştaki `GET /auth/me` çağrısında access token süresi dolmuşsa, refresh token'la sessizce
 * yenilenir (bkz. decisions.md → "Oturum Yönetimi & Token Yenileme").
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val authRepository: AuthRepository,
    private val licenseRepository: LicenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun onIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Retry -> restoreSession()
        }
    }

    /**
     * Oturum geri-yükleme akışı:
     * - Saklı access token yok → [SplashDestination.ONBOARDING].
     * - Token var → `GET /auth/me` (gerekiyorsa TokenAuthenticator refresh'i devreye girer):
     *   - başarılı → rol/ehliyet durumuna göre [resolveDestination].
     *   - 401 → refresh de başarısız; SessionManager token'ları temizledi → [SplashDestination.LOGIN].
     *   - ağ/bilinmeyen hata → oturum doğrulanamadı; tahmin yürütmeden (§2.2) tekrar-dene ekranı.
     */
    private fun restoreSession() {
        _uiState.update { it.copy(isLoading = true, isError = false, destination = null) }
        viewModelScope.launch {
            val token = tokenStore.currentAccessToken()
            if (token.isNullOrBlank()) {
                emit(SplashDestination.ONBOARDING)
                return@launch
            }
            authRepository.me()
                .onSuccess { user -> emit(resolveDestination(user.role)) }
                .onFailure { e ->
                    if (e is HttpException && e.code() == 401) {
                        emit(SplashDestination.LOGIN)
                    } else {
                        _uiState.update { it.copy(isLoading = false, isError = true) }
                    }
                }
        }
    }

    /**
     * Geçerli oturumda iniş hedefi. **OtpVerificationViewModel.resolveDestination ile AYNI kural**
     * (ikisi birlikte güncel tutulmalı): onaylı roller (CUSTOMER/ADMIN) → HOME. PENDING'de
     * `GET /license/status`: UNDER_REVIEW → LICENSE_PENDING, APPROVED → HOME, diğer/bilinmez →
     * LICENSE_UPLOAD (durum alınamazsa güvenli varsayılan: ehliyet yükleme).
     */
    private suspend fun resolveDestination(role: String): SplashDestination {
        if (role != "PENDING") return SplashDestination.HOME
        return when (licenseRepository.getStatus().getOrNull()) {
            LicenseVerificationStatus.UNDER_REVIEW -> SplashDestination.LICENSE_PENDING
            LicenseVerificationStatus.APPROVED -> SplashDestination.HOME
            else -> SplashDestination.LICENSE_UPLOAD
        }
    }

    /** Ekran, [SplashUiState.destination]'ı navigasyonda tüketince çağrılır (tekrar geçişi önler). */
    fun onDestinationHandled() {
        _uiState.update { it.copy(destination = null) }
    }

    private fun emit(destination: SplashDestination) {
        _uiState.update { it.copy(isLoading = false, isError = false, destination = destination) }
    }
}
