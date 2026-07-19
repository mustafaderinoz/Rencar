package com.turkcell.rencar.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.data.repository.LicenseRepository
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Profil ekranının tek durum kaynağı (§4.4). GET /auth/me ile ad/telefon, GET /license/status
 * ile ehliyet doğrulama durumu çekilir (statik değil). Ayrı domain/UseCase katmanı yoktur
 * (data + repository, decisions.md). "Çıkış yap" onay pop-up'ıyla POST /auth/logout'a bağlıdır;
 * menü satırları bu iş kapsamında bağlanmaz (§4.6).
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val licenseRepository: LicenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            ProfileIntent.Load, ProfileIntent.Retry -> load()
            ProfileIntent.LogoutClicked -> _uiState.update { it.copy(showLogoutConfirm = true) }
            ProfileIntent.DismissLogout -> _uiState.update { it.copy(showLogoutConfirm = false) }
            ProfileIntent.ConfirmLogout -> logout()
        }
    }

    /**
     * Oturumu kapatır: [AuthRepository.logout] sunucu oturumlarını iptal edip YEREL oturumu kapatır;
     * bunun yayınladığı oturum-sonu olayını NavHost dinleyip kullanıcıyı login'e yönlendirir (ekran
     * bu sırada zaten geri yığından çıkar). Bu yüzden başarı/hata için ayrı state güncellemesi yok;
     * yalnız tekrar-basmaya karşı [ProfileUiState.isLoggingOut] koruması tutulur.
     */
    private fun logout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    /**
     * Profili yükler: önce GET /auth/me (başlığı süren zorunlu veri), ardından GET /license/status.
     * Ehliyet durumu alınamazsa profil yine çizilir ([LicenseVerificationStatus.UNKNOWN]); yalnızca
     * `me` başarısızsa hata gösterilir.
     */
    private fun load() {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.me()
                .onSuccess { user ->
                    val status = licenseRepository.getStatus().getOrNull() ?: LicenseVerificationStatus.UNKNOWN
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            fullName = user.fullName,
                            phone = formatPhone(user.phone),
                            licenseStatus = status,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = e.toAppError().toUserMessage(ErrorContext.PROFILE),
                        )
                    }
                }
        }
    }

    /**
     * E.164 telefonu ("+905320000000") gösterim biçimine çevirir: "+90 532 000 00 00".
     * Ülke kodu (90) ayıklanır, kalan 10 hane 3-3-2-2 gruplanır; biçime uymuyorsa ham değer döner.
     */
    private fun formatPhone(raw: String?): String {
        val digits = raw?.filter(Char::isDigit) ?: return ""
        val local = digits.removePrefix("90").takeLast(10)
        if (local.length != 10) return raw.orEmpty()
        val grouped = buildString {
            local.forEachIndexed { i, c ->
                append(c)
                if (i == 2 || i == 5 || i == 7) append(' ')
            }
        }
        return "+90 $grouped"
    }
}
