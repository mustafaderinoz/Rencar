package com.turkcell.rencar.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Uygulama ömrü boyunca yaşayan tekil [CoroutineScope].
 *
 * ViewModel temizlense de tamamlanması gereken işler için (ör. foto ekranından başlatmadan
 * çıkınca PREPARING kiralamayı iptal eden DELETE) kullanılır; `viewModelScope` bu tür işi
 * geçiş anında iptal ederdi. [SupervisorJob] ile bir işin hatası diğerlerini düşürmez.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
