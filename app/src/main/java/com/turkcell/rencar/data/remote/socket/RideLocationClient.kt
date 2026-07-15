package com.turkcell.rencar.data.remote.socket

import com.turkcell.rencar.BuildConfig
import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.model.VehiclePoint
import com.turkcell.rencar.data.remote.session.SessionManager
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Aktif yolculuktaki aracın CANLI konumunu dinleyen Socket.IO istemcisi
 * (admin'in LocationSocketClient kalıbının müşteri karşılığı).
 *
 * Sunucu sözleşmesi: '/ws/locations' namespace'ine CUSTOMER token'ıyla bağlanılır; yalnız KENDİ
 * aktif kiralamasındaki aracın karesi 'my-vehicle' event'iyle gelir
 * (payload: { ts, vehicle: { vehicleId, latitude, longitude, ... } }). Aktif kiralama yoksa event
 * HİÇ gelmez — akış sessiz kalır, ekran haritayı çizmez.
 *
 * AUTH: token handshake'te auth.token ile gider. Handshake sırasında access token'ın süresi
 * dolmuşsa sunucu bağlantıyı reddeder (connect_error). Bu durumda [SessionManager.refreshSession] ile
 * taze token alınıp SINIRLI sayıda yeniden bağlanılır (token, socket oluşturulurken okunduğundan
 * yenilemeden sonra socket taze token'la yeniden kurulur). Refresh de başarısızsa akış sessiz kalır.
 *
 * Örnek koddan sapmalar (AGENTS §2.2 "uydurmak yasak" + decisions.md "Minimum Değişiklik"):
 * - TokenStore'da `accessToken()` yok; suspend [TokenStore.currentAccessToken] kullanılır.
 * - [BuildConfig.BASE_URL] sonda '/' içerir; namespace eklenmeden önce kırpılır (çift slash olmasın).
 * - Yenileme-yeniden bağlanma [MAX_AUTH_RETRIES] ile sınırlanır; başarılı bağlantıda ([Socket.EVENT_CONNECT])
 *   sayaç sıfırlanır. Böylece auth dışı (ağ) kaynaklı connect_error'lar sıkı bir refresh döngüsü kurmaz.
 *
 * Kütüphane (io.socket) yalnızca bu data-katmanı sınıfında geçer; UI Flow<VehiclePoint> görür
 * (decisions.md → "Kütüphane kullanımı repository/di ardında; UI kütüphaneye bağımlı kalmaz").
 */
@Singleton
class RideLocationClient @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionManager: SessionManager,
) {
    fun vehiclePositionStream(): Flow<VehiclePoint> = callbackFlow {
        var socket: Socket? = null
        // Auth kaynaklı yeniden bağlanma sayacı. Callback'lerden tetiklenen coroutine'ler bu akışın
        // dispatcher'ında (collector; genelde Main) sıralı koştuğundan pratikte serileşir.
        var authRetries = 0

        fun teardown() {
            socket?.let {
                it.off()
                it.disconnect()
                it.close()
            }
            socket = null
        }

        // Verilen token'la taze bir socket kurar (varsa öncekini kapatır) ve olaylarını bağlar.
        fun connectWith(token: String) {
            teardown()
            val opts = IO.Options().apply {
                auth = mapOf("token" to token)
                forceNew = true
                reconnection = true
            }
            runCatching { IO.socket(BuildConfig.BASE_URL.trimEnd('/') + NAMESPACE, opts) }
                .onSuccess { s ->
                    s.on(MY_VEHICLE_EVENT) { args ->
                        parsePoint(args)?.let { trySend(it) }
                    }
                    // Bağlantı kurulunca sayacı sıfırla: geçici hatalar oturum boyunca yenilemeyi kilitlemesin.
                    s.on(Socket.EVENT_CONNECT) { authRetries = 0 }
                    // Handshake reddi (ör. token süresi doldu) → sınırlı sayıda tazele + yeniden bağlan.
                    s.on(Socket.EVENT_CONNECT_ERROR) {
                        if (authRetries++ < MAX_AUTH_RETRIES) {
                            launch {
                                val ok = sessionManager.refreshSession(token)
                                val fresh = tokenStore.currentAccessToken()
                                // Yalnız token GERÇEKTEN değiştiyse yeniden bağlan (sıkı döngü olmasın).
                                if (ok && !fresh.isNullOrBlank() && fresh != token) {
                                    connectWith(fresh)
                                }
                            }
                        }
                    }
                    socket = s
                    s.connect()
                }
                // URI hatası vb. → akışı sessizce kapat (harita çizilmez, ekran çalışmaya devam eder).
                .onFailure { close() }
        }

        val token = tokenStore.currentAccessToken()
        if (token.isNullOrBlank()) close() else connectWith(token)

        awaitClose { teardown() }
    }

    private fun parsePoint(args: Array<Any?>): VehiclePoint? {
        val root = args.getOrNull(0) as? JSONObject ?: return null
        val vehicle = root.optJSONObject("vehicle") ?: return null
        val lat = vehicle.optDouble("latitude", Double.NaN)
        val lng = vehicle.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lng.isNaN()) return null
        return VehiclePoint(latitude = lat, longitude = lng)
    }

    private companion object {
        const val NAMESPACE = "/ws/locations"
        const val MY_VEHICLE_EVENT = "my-vehicle"
        const val MAX_AUTH_RETRIES = 2
    }
}
