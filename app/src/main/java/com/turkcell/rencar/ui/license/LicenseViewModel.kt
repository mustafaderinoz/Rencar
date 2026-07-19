package com.turkcell.rencar.ui.license

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Ehliyet doğrulama 1. adımının ViewModel'i. Android API'lerine (kamera/FileProvider)
 * dokunmaz; ekran katmanı çekim yapıp dosya yolunu intent ile buraya iletir (§4.4 saf VM).
 */
@HiltViewModel
class LicenseViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(LicenseUiState())
    val uiState: StateFlow<LicenseUiState> = _uiState.asStateFlow()

    fun onIntent(intent: LicenseIntent) {
        when (intent) {
            is LicenseIntent.FrontCaptured ->
                _uiState.update { it.copy(frontPath = intent.path).withContinueFlag() }

            is LicenseIntent.BackCaptured ->
                _uiState.update { it.copy(backPath = intent.path).withContinueFlag() }

            LicenseIntent.ContinueClicked ->
                _uiState.update {
                    if (it.canContinue) it.copy(proceed = true) else it
                }

            // Navigasyon ekran katmanında ele alınır.
            LicenseIntent.BackClicked -> Unit

            // Ekran geçişi yaptı → bayrağı tüket (tekrar geçişi önler).
            LicenseIntent.ProceedHandled -> _uiState.update { it.copy(proceed = false) }
        }
    }

    private fun LicenseUiState.withContinueFlag(): LicenseUiState =
        copy(canContinue = frontPath != null && backPath != null)
}
