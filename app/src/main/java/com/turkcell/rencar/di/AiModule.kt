package com.turkcell.rencar.di

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.turkcell.rencar.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Gemini üretken model sağlayıcısı.
 *
 * Kütüphane (`GenerativeModel`) DI ardında tutulur (decisions.md → "Minimum Değişiklik İlkesi"
 * / Kütüphane): [com.turkcell.rencar.data.repository.AiRepository] SDK'yı gövdesinde KURMAZ,
 * buradan enjekte alır. Böylece model yapılandırması tek noktada toplanır ve repository, sahte
 * bir model verilerek test edilebilir hâle gelir (diğer istemcilerin [NetworkModule]'de olması gibi).
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideGenerativeModel(): GenerativeModel = GenerativeModel(
        modelName = "gemini-3.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        },
    )
}
