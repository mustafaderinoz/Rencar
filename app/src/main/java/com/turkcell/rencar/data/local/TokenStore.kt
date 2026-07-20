package com.turkcell.rencar.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Access + refresh token'ların DataStore üzerinde saklanması (karar: decisions.md).
 * Auth interceptor access token'ı buradan senkron okur (runBlocking + first()).
 */
@Singleton
class TokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val accessTokenKey = stringPreferencesKey("access_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    val accessToken: Flow<String?> = dataStore.data.map { it[accessTokenKey] }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[accessTokenKey] = accessToken
            prefs[refreshTokenKey] = refreshToken
        }
    }

    /** Interceptor için tek seferlik okuma. */
    suspend fun currentAccessToken(): String? = dataStore.data.first()[accessTokenKey]

    /** SessionManager'ın refresh çağrısı için tek seferlik okuma (POST /auth/refresh gövdesi). */
    suspend fun currentRefreshToken(): String? = dataStore.data.first()[refreshTokenKey]

    /**
     * Yalnız oturum anahtarlarını siler — DataStore'un tamamı DEĞİL. Aynı store'da oturumdan
     * bağımsız tercihler de tutulur (bkz. [ThemeStore]); `clear()` çağrılsaydı çıkış yapan
     * kullanıcının tema seçimi de silinirdi.
     */
    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(accessTokenKey)
            prefs.remove(refreshTokenKey)
        }
    }
}
