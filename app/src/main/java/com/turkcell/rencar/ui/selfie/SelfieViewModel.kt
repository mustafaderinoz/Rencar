package com.turkcell.rencar.ui.selfie

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.repository.LicenseRepository
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.util.ErrorContext
import com.turkcell.rencar.util.FormMessages
import com.turkcell.rencar.util.toAppError
import com.turkcell.rencar.util.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Selfie doğrulama ViewModel'i. Ehliyet yollarını nav argümanından [SavedStateHandle] ile
 * alır (OTP ekranının `phone` kalıbı ile aynı). Yüz ~[HOLD_STEPS]×[STEP_MS] ms ortalı
 * kalınca ehliyet ön+arka görsellerini yükler. Android/kamera API'lerine dokunmaz (§4.4).
 */
@HiltViewModel
class SelfieViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val licenseRepository: LicenseRepository,
) : ViewModel() {

    private val frontPath: String =
        savedStateHandle.get<String>(RencarDestinations.SELFIE_ARG_FRONT).orEmpty()
    private val backPath: String =
        savedStateHandle.get<String>(RencarDestinations.SELFIE_ARG_BACK).orEmpty()

    private val _uiState = MutableStateFlow(SelfieUiState())
    val uiState: StateFlow<SelfieUiState> = _uiState.asStateFlow()

    // Yüz ortalı tutuldukça ilerleyen "sabit tut" işi; ortalanma bozulunca iptal edilir.
    private var holdJob: Job? = null

    fun onIntent(intent: SelfieIntent) {
        when (intent) {
            is SelfieIntent.PermissionResult ->
                _uiState.update {
                    it.copy(permissionGranted = intent.granted, permissionRequested = true)
                }

            is SelfieIntent.FaceStatusChanged -> onFaceStatus(intent.status)

            is SelfieIntent.SelfieCaptured -> onSelfieCaptured(intent.path)

            SelfieIntent.SelfieCaptureFailed ->
                _uiState.update {
                    it.copy(
                        isUploading = false,
                        captureRequested = false,
                        holdProgress = 0f,
                        faceStatus = FaceStatus.NoFace,
                        errorMessage = FormMessages.SELFIE_CAPTURE_FAILED,
                    )
                }

            SelfieIntent.RetryClicked -> {
                holdJob?.cancel()
                holdJob = null
                _uiState.update {
                    it.copy(
                        faceStatus = FaceStatus.NoFace,
                        holdProgress = 0f,
                        captureRequested = false,
                        errorMessage = null,
                        isUploading = false,
                    )
                }
            }

            // Navigasyon ekran katmanında ele alınır.
            SelfieIntent.BackClicked -> Unit
            SelfieIntent.DoneClicked -> Unit
        }
    }

    private fun onFaceStatus(status: FaceStatus) {
        val state = _uiState.value
        // Yükleme başladıysa / bittiyse / hata varsa yeni kare değerlendirilmez.
        if (state.isUploading || state.uploaded || state.errorMessage != null) return

        _uiState.update { it.copy(faceStatus = status) }

        if (status == FaceStatus.Centered) {
            // Zaten sayaç işliyorsa yeniden başlatma.
            if (holdJob?.isActive != true) startHold()
        } else {
            holdJob?.cancel()
            holdJob = null
            if (state.holdProgress != 0f) _uiState.update { it.copy(holdProgress = 0f) }
        }
    }

    /** Yüz ortalı kaldığı sürece ilerlemeyi doldurur; tamamlanınca selfie çekimini tetikler. */
    private fun startHold() {
        holdJob = viewModelScope.launch {
            for (step in 1..HOLD_STEPS) {
                delay(STEP_MS)
                _uiState.update { it.copy(holdProgress = step.toFloat() / HOLD_STEPS) }
            }
            // Yüz yeterince ortalı kaldı → ekran katmanı ön kameradan selfie karesini çeksin.
            // isUploading true: "Doğrulanıyor…" gösterilir ve sonraki kareler değerlendirilmez.
            _uiState.update { it.copy(isUploading = true, captureRequested = true, errorMessage = null) }
        }
    }

    /** Selfie karesi çekildi → ehliyet ön+arka + selfie'yi yükle. */
    private fun onSelfieCaptured(path: String) {
        _uiState.update { it.copy(captureRequested = false) }
        viewModelScope.launch {
            licenseRepository.upload(
                front = File(frontPath),
                back = File(backPath),
                selfie = File(path),
            )
                .onSuccess {
                    _uiState.update { it.copy(isUploading = false, uploaded = true) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            holdProgress = 0f,
                            faceStatus = FaceStatus.NoFace,
                            errorMessage = e.toAppError()
                                .toUserMessage(ErrorContext.SELFIE_UPLOAD),
                        )
                    }
                }
        }
    }

    private companion object {
        // ~1.2 sn ortalı tutulunca yükleme tetiklenir.
        const val HOLD_STEPS = 24
        const val STEP_MS = 50L
    }
}
