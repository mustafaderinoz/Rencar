package com.turkcell.rencar.ui.onboarding

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onIntent(intent: OnboardingIntent) {
        when (intent) {
            is OnboardingIntent.PageChanged ->
                _uiState.update { it.copy(currentPage = intent.page) }

            // §4.6: navigasyon/effect katmanı varsayılan olarak eklenmez; yalnızca state.
            OnboardingIntent.StartClicked -> Unit
            OnboardingIntent.LoginClicked -> Unit
        }
    }
}
