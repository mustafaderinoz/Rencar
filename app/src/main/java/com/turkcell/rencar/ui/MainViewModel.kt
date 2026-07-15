package com.turkcell.rencar.ui

import androidx.lifecycle.ViewModel
import com.turkcell.rencar.data.remote.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow

/**
 * Uygulama seviyesi oturum gözcüsü. [SessionManager.forcedLogout] olayını dışa açar; NavHost bunu
 * dinleyip kullanıcıyı login'e yönlendirir (refresh token da ölünce otomatik kurtarma imkânsızdır).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    sessionManager: SessionManager,
) : ViewModel() {

    /** Otomatik kurtarılamayan oturum sonu olayı. */
    val forcedLogout: SharedFlow<Unit> = sessionManager.forcedLogout
}
