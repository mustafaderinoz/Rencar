package com.turkcell.rencar.data.remote.socket

import com.turkcell.rencar.BuildConfig
import com.turkcell.rencar.data.local.TokenStore
import com.turkcell.rencar.data.model.VehiclePoint
import io.socket.client.IO
import io.socket.client.Socket
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
 * AUTH: token handshake'te auth.token ile gider.
 *
 * Örnek koddan sapmalar (AGENTS §2.2 "uydurmak yasak" + decisions.md "Minimum Değişiklik"):
 * - TokenStore'da `accessToken()` yok; suspend [TokenStore.currentAccessToken] kullanılır.
 * - [BuildConfig.BASE_URL] sonda '/' içerir; namespace eklenmeden önce kırpılır (çift slash olmasın).
 * - Projede `SessionManager`/token-refresh ucu YOK; örnekteki oturum-tazeleme dalı çıkarıldı.
 *   Bağlantı hatasında Socket.IO'nun kendi `reconnection`'ına bırakılır (olmayan uç uydurulmaz).
 *
 * Kütüphane (io.socket) yalnızca bu data-katmanı sınıfında geçer; UI Flow<VehiclePoint> görür
 * (decisions.md → "Kütüphane kullanımı repository/di ardında; UI kütüphaneye bağımlı kalmaz").
 */
@Singleton
class RideLocationClient @Inject constructor(
    private val tokenStore: TokenStore,
) {
    fun vehiclePositionStream(): Flow<VehiclePoint> = callbackFlow {
        var socket: Socket? = null

        fun teardown() {
            socket?.let {
                it.off()
                it.disconnect()
                it.close()
            }
            socket = null
        }

        val token = tokenStore.currentAccessToken()
        if (token.isNullOrBlank()) {
            close()
        } else {
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
                    socket = s
                    s.connect()
                }
                // URI hatası vb. → akışı sessizce kapat (harita çizilmez, ekran çalışmaya devam eder).
                .onFailure { close() }
        }

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
    }
}
