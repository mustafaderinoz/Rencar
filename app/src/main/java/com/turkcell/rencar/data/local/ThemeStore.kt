package com.turkcell.rencar.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Kullanıcının koyu/açık tema tercihinin DataStore üzerinde saklanması (karar: decisions.md
 * "Tema Modu"). [TokenStore] ile AYNI DataStore örneğini kullanır; ayrı bir dosya açılmaz.
 *
 * Değer üç durumludur: `null` = kullanıcı henüz seçmedi (sistem ayarı takip edilir),
 * `true`/`false` = kullanıcının profil ekranından yaptığı açık seçim.
 */
@Singleton
class ThemeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    /** Kayıtlı tercih; `null` ise sistem temasına düşülür. */
    val darkTheme: Flow<Boolean?> = dataStore.data.map { it[darkThemeKey] }

    suspend fun setDarkTheme(dark: Boolean) {
        dataStore.edit { it[darkThemeKey] = dark }
    }
}
