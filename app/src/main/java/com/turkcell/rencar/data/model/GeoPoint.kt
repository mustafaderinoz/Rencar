package com.turkcell.rencar.data.model

/**
 * Coğrafi nokta (enlem/boylam) — UI/domain modeli.
 *
 * Harita sözleşmesini ([com.turkcell.rencar.ui.map.MapUiState] + MapIntent) harita motoru tipinden
 * (MapLibre `LatLng`) bağımsız tutar: decisions.md → "Minimum Değişiklik İlkesi / Kütüphane" ("UI
 * kütüphaneye doğrudan bağımlı kalmaz"). Motor tipine dönüşüm YALNIZ harita sınırında (Screen) yapılır.
 *
 * Not: [VehiclePoint] ile alan yapısı aynıdır ama ayrı bir kavramdır (o, socket'ten gelen AKTİF ARAÇ
 * konumu; bu, kullanıcının kendi konumu). İleride tek bir coğrafi tipte birleştirilebilir.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
)
