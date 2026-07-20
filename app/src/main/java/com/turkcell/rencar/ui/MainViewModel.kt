package com.turkcell.rencar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turkcell.rencar.data.local.ThemeStore
import com.turkcell.rencar.data.remote.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Uygulama seviyesi oturum gözcüsü. [SessionManager.forcedLogout] olayını dışa açar; NavHost bunu
 * dinleyip kullanıcıyı login'e yönlendirir (refresh token da ölünce otomatik kurtarma imkânsızdır).
 *
 * Ayrıca uygulamanın TEK tema kaynağıdır: [darkTheme] tercihini MainActivity dinleyip
 * `RenCarTheme`'e verir; profil ekranındaki toggle bu tercihi [ThemeStore]'a yazdığı için
 * değişiklik tüm ekranlara anında yansır (bkz. decisions.md "Tema Modu").
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    sessionManager: SessionManager,
    themeStore: ThemeStore,
) : ViewModel() {

    /** Otomatik kurtarılamayan oturum sonu olayı. */
    val forcedLogout: SharedFlow<Unit> = sessionManager.forcedLogout

    /** Kullanıcının tema tercihi; `null` iken sistem teması geçerlidir. */
    val darkTheme: StateFlow<Boolean?> = themeStore.darkTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )
}
