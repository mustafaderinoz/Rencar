package com.turkcell.rencar.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.turkcell.rencar.BuildConfig
import com.turkcell.rencar.data.remote.api.AuthApi
import com.turkcell.rencar.data.remote.api.LicenseApi
import com.turkcell.rencar.data.remote.api.RefreshApi
import com.turkcell.rencar.data.remote.api.RentalApi
import com.turkcell.rencar.data.remote.api.ReservationApi
import com.turkcell.rencar.data.remote.api.VehicleApi
import com.turkcell.rencar.data.remote.interceptor.AuthInterceptor
import com.turkcell.rencar.data.remote.interceptor.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

/** Ağ katmanı sağlayıcıları: Json, OkHttp, Retrofit, AuthApi. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        // 401 alınca access token'ı sessizce yenileyip isteği tekrar dener (bkz. TokenAuthenticator).
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor())
        .build()

    /**
     * Yalnız token yenileme için AYRI, sade istemci: AuthInterceptor ve Authenticator İÇERMEZ.
     * Bu ayrım (a) SessionManager ↔ ana istemci Authenticator'ı arasındaki dairesel bağımlılığı kırar,
     * (b) refresh çağrısı 401 dönse bile kendini yeniden tetiklemesini önler.
     * İstemci + Retrofit burada izole edilir; grafikte tek public OkHttpClient/Retrofit kalır.
     */
    @Provides
    @Singleton
    fun provideRefreshApi(json: Json): RefreshApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return retrofit.create(RefreshApi::class.java)
    }

    private fun loggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideLicenseApi(retrofit: Retrofit): LicenseApi = retrofit.create(LicenseApi::class.java)

    @Provides
    @Singleton
    fun provideVehicleApi(retrofit: Retrofit): VehicleApi = retrofit.create(VehicleApi::class.java)

    @Provides
    @Singleton
    fun provideReservationApi(retrofit: Retrofit): ReservationApi =
        retrofit.create(ReservationApi::class.java)

    @Provides
    @Singleton
    fun provideRentalApi(retrofit: Retrofit): RentalApi = retrofit.create(RentalApi::class.java)
}
