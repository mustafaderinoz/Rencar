package com.turkcell.rencar.ui.map

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/** Ege / İzmir civarı varsayılan kamera merkezi (konum gelene kadar gösterilir). */
val DEFAULT_CENTER: LatLng = LatLng(38.51740367746754, 27.161930350129918)

private const val DEFAULT_ZOOM: Double = 10.0

private val ME_MARKER_COLOR = Color.parseColor("#4285F4")

/** OSM raster kaynağı kullanan minimal MapLibre stili (harici sunucu gerektirmez). */
private const val OSM_STYLE_JSON: String = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": [
        "https://a.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://b.tile.openstreetmap.org/{z}/{x}/{y}.png",
        "https://c.tile.openstreetmap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "attribution": "©️ OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
"""

/**
 * Haritaya dışarıdan imperatif komut vermeyi sağlar (kamera hareketi). Screen katmanı
 * [rememberRencarMapController] ile üretir; FAB/ilk-konum akışında [animateTo] çağırır.
 */
class RencarMapController internal constructor() {
    internal var map: MapLibreMap? = null

    fun animateTo(target: LatLng, zoom: Double = DEFAULT_ZOOM) {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom))
    }
}

@Composable
fun rememberRencarMapController(): RencarMapController = remember { RencarMapController() }

/**
 * MapLibre [MapView] ↔ Compose köprüsü. Yalnızca haritayı çizer ve [myLocation] değiştikçe
 * "konumum" noktasını günceller; kamerayı kendisi oynatmaz (bu, controller ile Screen'de
 * yapılır). Önizlemede (LocalInspectionMode) native motor başlatılmaz, placeholder gösterilir.
 */
@Composable
fun RencarMap(
    myLocation: LatLng?,
    modifier: Modifier = Modifier,
    initialCenter: LatLng = DEFAULT_CENTER,
    initialZoom: Double = DEFAULT_ZOOM,
    controller: RencarMapController? = null,
) {
    // @Preview: MapLibre native motoru render edilemez → basit placeholder.
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Harita önizlemesi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context) // Native motoru başlat.
        MapView(context).apply { onCreate(null) }
    }

    var mapAndStyle by remember { mutableStateOf<Pair<MapLibreMap, Style>?>(null) }

    // Harita görünümünü uygulama yaşam döngüsüne bağla; ekran yıkılırken temizle.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    // Harita + stil kurulumu -> yalnızca bir kez.
    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            controller?.map = map

            map.cameraPosition = CameraPosition.Builder()
                .target(initialCenter)
                .zoom(initialZoom)
                .build()

            map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) { loaded ->
                loaded.addSource(GeoJsonSource("me"))
                // Dış halka -> konumun etrafında yumuşak, yarı saydam halo.
                loaded.addLayer(
                    CircleLayer("me-halo-layer", "me").withProperties(
                        PropertyFactory.circleColor(ME_MARKER_COLOR),
                        PropertyFactory.circleRadius(20f),
                        PropertyFactory.circleOpacity(0.2f),
                        PropertyFactory.circleBlur(0.4f),
                    ),
                )
                // İç nokta -> beyaz kenarlıklı mavi nokta ("my location" tarzı).
                loaded.addLayer(
                    CircleLayer("me-layer", "me").withProperties(
                        PropertyFactory.circleColor(ME_MARKER_COLOR),
                        PropertyFactory.circleRadius(9f),
                        PropertyFactory.circleStrokeColor(Color.WHITE),
                        PropertyFactory.circleStrokeWidth(3f),
                    ),
                )
                mapAndStyle = map to loaded
            }
        }
    }

    // Konum noktası -> myLocation her değiştiğinde güncellenir, kamera oynamaz.
    LaunchedEffect(mapAndStyle, myLocation) {
        val (_, style) = mapAndStyle ?: return@LaunchedEffect
        updateMe(style, myLocation)
    }

    // AndroidView -> Android View ↔ @Composable köprüsü.
    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}

/** "me" GeoJSON kaynağını konuma göre günceller; konum yoksa noktayı gizler. */
private fun updateMe(style: Style, myLocation: LatLng?) {
    val source = style.getSourceAs<GeoJsonSource>("me") ?: return
    if (myLocation == null) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        source.setGeoJson(Point.fromLngLat(myLocation.longitude, myLocation.latitude))
    }
}
