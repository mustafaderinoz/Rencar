package com.turkcell.rencar.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.model.LicenseVerificationStatus
import com.turkcell.rencar.data.repository.AuthRepository
import com.turkcell.rencar.data.repository.LicenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/**
 * Profil ekranının tek durum kaynağı (§4.4). GET /auth/me ile ad/telefon, GET /license/status
 * ile ehliyet doğrulama durumu çekilir (statik değil). Ayrı domain/UseCase katmanı yoktur
 * (data + repository, decisions.md). Menü/çıkış aksiyonları bu iş kapsamında bağlanmaz (§4.6).
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
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.toMessage()) }
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

    private fun Throwable.toMessage(): String = when (this) {
        is HttpException -> when (code()) {
            401 -> "Oturum bulunamadı. Lütfen tekrar giriş yapın."
            else -> "Profil yüklenemedi (${code()}). Lütfen tekrar deneyin."
        }
        is IOException -> "İnternet bağlantısı kurulamadı."
        else -> "Beklenmeyen bir hata oluştu."
    }
}
