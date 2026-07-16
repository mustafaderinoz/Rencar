package com.turkcell.rencar.data.remote.api

import com.turkcell.rencar.data.remote.dto.CardResponse
import com.turkcell.rencar.data.remote.dto.CreateCardRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Kayıtlı kart uçları (openapi.json — tag: Cards). Auth zorunlu (AuthInterceptor Bearer ekler),
 * yalnızca CUSTOMER rolü erişebilir. Kart YALNIZ görsel meta olarak tutulur (marka + son 4 hane + SKT).
 */
interface CardApi {

    /** Kayıtlı kartlar (CardsController_list): öntanımlı en üstte, gerisi yeniden eskiye. */
    @GET("cards")
    suspend fun list(): List<CardResponse>

    /**
     * Yeni kart kaydeder (CardsController_create). Yalnız marka + son 4 hane + SKT gönderilir;
     * tam kart numarası gönderilirse 400. İlk kart otomatik öntanımlı olur.
     */
    @POST("cards")
    suspend fun create(@Body body: CreateCardRequest): CardResponse

    /** Kartı öntanımlı yapar (CardsController_setDefault); önceki öntanımlının işareti kalkar. */
    @PATCH("cards/{id}/default")
    suspend fun setDefault(@Path("id") cardId: String): CardResponse

    /** Kartı siler (CardsController_remove, 204). Gövdesiz yanıt olduğundan [Response] ile alınır. */
    @DELETE("cards/{id}")
    suspend fun remove(@Path("id") cardId: String): Response<Unit>
}
