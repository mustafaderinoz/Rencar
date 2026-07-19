package com.turkcell.rencar.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.data.repository.LicenseRepository
import com.turkcell.rencar.data.repository.RentalRepository
import com.turkcell.rencar.data.repository.ReservationRepository
import com.turkcell.rencar.util.isUnauthorized
import com.turkcell.rencar.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val rentalRepository: RentalRepository,
    private val reservationRepository: ReservationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        restoreSession()
    }

    fun onIntent(intent: SplashIntent) {
        when (intent) {
            SplashIntent.Retry -> restoreSession()

            // Ekran geçişi yaptı → hedefi tüket (tekrar geçişi önler).
            SplashIntent.DestinationHandled -> _uiState.update { it.copy(destination = null) }
        }
    }

    /**
     * Oturum geri-yükleme akışı:
     * - Saklı access token yok → [SplashDestination.Onboarding].
     * - Token var → `GET /auth/me` (gerekiyorsa TokenAuthenticator refresh'i devreye girer):
     *   - başarılı → rol/ehliyet durumuna göre [resolveDestination].
     *   - 401 → refresh de başarısız; SessionManager token'ları temizledi → [SplashDestination.Login].
     *   - ağ/bilinmeyen hata → oturum doğrulanamadı; tahmin yürütmeden (§2.2) tekrar-dene ekranı.
     */
    private fun restoreSession() {
        _uiState.update { it.copy(isLoading = true, isError = false, destination = null) }
        viewModelScope.launch {
            val token = tokenStore.currentAccessToken()
            if (token.isNullOrBlank()) {
                emit(SplashDestination.Onboarding)
                return@launch
            }
            authRepository.me()
                .onSuccess { user -> emit(resolveDestination(user.role)) }
                .onFailure { e ->
                    if (e.toAppError().isUnauthorized) {
                        emit(SplashDestination.Login)
                    } else {
                        _uiState.update { it.copy(isLoading = false, isError = true) }
                    }
                }
        }
    }

    /**
     * Geçerli oturumda iniş hedefi. PENDING/ehliyet kuralı **OtpVerificationViewModel.resolveDestination
     * ile AYNIdır** (ikisi birlikte güncel tutulmalı): PENDING'de `GET /license/status`: UNDER_REVIEW →
     * LicensePending, APPROVED → Home, diğer/bilinmez → LicenseUpload. Onaylı rollerde HOME döner.
     *
     * **Splash'e ÖZEL (Otp'de yok):** CUSTOMER için Home'dan önce yeniden açılış kurtarması yapılır
     * (aktif yolculuk / yarım foto akışı / aktif rezervasyon). Bu, açılışa özgü bir davranıştır; giriş
     * sonrası (Otp) akışta kullanıcı zaten ilgili ekrandan gelir, o yüzden orada replike EDİLMEZ.
     */
    private suspend fun resolveDestination(role: String): SplashDestination {
        if (role == "PENDING") {
            return when (licenseRepository.getStatus().getOrNull()) {
                LicenseVerificationStatus.UNDER_REVIEW -> SplashDestination.LicensePending
                LicenseVerificationStatus.APPROVED -> SplashDestination.Home
                else -> SplashDestination.LicenseUpload
            }
        }
        // ADMIN'in kiralaması yoktur → doğrudan Home; CUSTOMER'da devam eden akış varsa oraya kurtar.
        if (role == "CUSTOMER") {
            resolveActiveFlow()?.let { return it }
        }
        return SplashDestination.Home
    }

    /**
     * CUSTOMER için yeniden açılışta devam eden akış kurtarma: önce kiralama (ACTIVE → Aktif Yolculuk,
     * PREPARING → Foto devralma), sonra aktif rezervasyon (→ Rezervasyon ekranı, geri sayım). Ağ/404
     * hatalarında sessizce null döner ve Home'a düşülür (kurtarma zorunlu değildir, açılışı bloklamaz).
     */
    private suspend fun resolveActiveFlow(): SplashDestination? {
        rentalRepository.findResumableRental().getOrNull()?.let { rental ->
            return if (rental.isActive) {
                SplashDestination.ActiveRental(rental.rentalId)
            } else {
                SplashDestination.PreparingRental(rental.vehicleId, rental.plan)
            }
        }
        reservationRepository.getActiveReservation().getOrNull()?.let { reservation ->
            return SplashDestination.ActiveReservation(reservation.vehicleId)
        }
        return null
    }

    private fun emit(destination: SplashDestination) {
        _uiState.update { it.copy(isLoading = false, isError = false, destination = destination) }
    }
}
