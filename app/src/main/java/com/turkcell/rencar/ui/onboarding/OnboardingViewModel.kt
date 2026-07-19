package com.turkcell.rencar.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            // §4.6: navigasyon/effect katmanı varsayılan olarak eklenmez; ikisi de Screen'de ele alınır.
            OnboardingIntent.StartClicked -> Unit
            OnboardingIntent.LoginClicked -> Unit
        }
    }
}
