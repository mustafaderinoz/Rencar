package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.VehicleResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Araç uçları (openapi.json — tag: Vehicles). Base URL: BuildConfig.BASE_URL. */
interface VehicleApi {

    /**
     * Müsait (AVAILABLE) araçları listeler. Auth zorunlu (AuthInterceptor Bearer ekler) ve
     * yalnızca CUSTOMER rolü erişebilir (PENDING → 403). Query'ler opsiyoneldir; null gönderilen
     * parametreler Retrofit tarafından atlanır.
     */
    @GET("vehicles")
    suspend fun list(
        @Query("type") type: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
    ): List<VehicleResponse>

    /**
     * Tek aracın detayını getirir (araç detay ekranı). AVAILABLE araçları herkes görür;
     * müsait olmayan araç yalnızca o araçta aktif kiralaması olan kullanıcıya görünür,
     * aksi halde 404 döner (VehicleController_getOne).
     */
    @GET("vehicles/{id}")
    suspend fun getOne(@Path("id") id: String): VehicleResponse
}
