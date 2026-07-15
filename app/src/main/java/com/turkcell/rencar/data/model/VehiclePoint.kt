package com.turkcell.rencar.data.model

/**
 * Aktif yolculuktaki aracın anlık konumu — Socket.IO 'my-vehicle' karesinin sadeleştirilmiş
 * modeli (decisions.md → "Katman Derinliği": UI/domain modeli; ham socket payload'ı UI'a sızmaz).
 *
 * [com.turkcell.rencar.data.remote.socket.RideLocationClient] JSON karesini bu modele çevirir;
 * Aktif Kiralama ekranı haritadaki araç marker'ını bununla besler.
 */
data class VehiclePoint(
    val latitude: Double,
    val longitude: Double,
)
