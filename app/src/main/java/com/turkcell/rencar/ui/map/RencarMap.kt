package com.turkcell.rencar.ui.map

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
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
import androidx.compose.runtime.rememberUpdatedState
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
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import com.turkcell.rencar.data.model.VehicleUi

/** Ege / İzmir civarı varsayılan kamera merkezi (konum gelene kadar gösterilir). */
val DEFAULT_CENTER: LatLng = LatLng(38.51740367746754, 27.161930350129918)

private const val DEFAULT_ZOOM: Double = 10.0

/** Aktif yolculukta araca odaklanınca kullanılan yakınlaştırma seviyesi. */
private const val RIDE_ZOOM: Double = 15.0

private val ME_MARKER_COLOR = Color.parseColor("#4285F4")

// ── Kümeleme (clustering) ─────────────────────────────────────────────────────────────────────
/** Kümeleme yarıçapı (PİKSEL). Sabit piksel olduğundan zoom arttıkça kapsanan coğrafi alan küçülür
 *  → kümeler zooma bağlı olarak ayrışır. Büyük değer = daha geniş kümeleme alanı. */
private const val CLUSTER_RADIUS: Int = 60

/** Bu zoom seviyesinin üstünde kümeleme yapılmaz; araçlar tek tek gösterilir. */
private const val CLUSTER_MAX_ZOOM: Int = 14

/** Kümeye dokununca kaç kademe yakınlaşılacağı (küme açılır). */
private const val CLUSTER_ZOOM_STEP: Double = 2.0

/** Kümeye dokunarak yakınlaşmada üst zoom sınırı. */
private const val MAX_CLUSTER_ZOOM: Double = 18.0

/** Küme sayı balonu ikon adı öneki: "cluster-<sayı>" (talep anında bitmap üretilir). */
private const val CLUSTER_IMAGE_PREFIX: String = "cluster-"

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

    /** Kamerayı bir kademe yakınlaştırır (+ butonu). */
    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    /** Kamerayı bir kademe uzaklaştırır (- butonu). */
    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }
}

@Composable
fun rememberRencarMapController(): RencarMapController = remember { RencarMapController() }

/**
 * MapLibre [MapView] ↔ Compose köprüsü. Yalnızca haritayı çizer ve [myLocation] değiştikçe
 * "konumum" noktasını günceller; kamerayı kendisi oynatmaz (bu, controller ile Screen'de
 * yapılır). Önizlemede (LocalInspectionMode) native motor başlatılmaz, placeholder gösterilir.
 *
 * Yeniden kullanım: bu composable home'a bağlı değildir; herhangi bir ekranda gömülebilir.
 * [ridePoint] verildiğinde (Aktif Yolculuk) tek bir araç pin'i çizilir ve kamera onu takip eder
 * ([myLocation]/[vehicles]'tan bağımsız). ridePoint null iken davranış home ile aynıdır.
 */
@Composable
fun RencarMap(
    myLocation: LatLng?,
    modifier: Modifier = Modifier,
    initialCenter: LatLng = DEFAULT_CENTER,
    initialZoom: Double = DEFAULT_ZOOM,
    controller: RencarMapController? = null,
    vehicles: List<VehicleUi> = emptyList(),
    recommendedVehicleIds: Set<String> = emptySet(),
    onVehicleClick: (String) -> Unit = {},
    ridePoint: LatLng? = null,
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
    // Tıklama listener'ı tek sefer kurulur; en güncel callback'i çağırması için State ile sarılır.
    val currentOnVehicleClick by rememberUpdatedState(onVehicleClick)

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

            // Sekme değişiminde (Profil↔Harita) bu composable atılıp yeniden yaratıldığında
            // MapView sıfırlanır; ama MapViewModel korunduğundan kullanıcı konumu hâlâ elimizdedir.
            // Kamerayı bilinen son konuma aç (yoksa default merkez) — aksi halde harita default
            // merkeze döner ve araçlar ekran dışında kalıp "kaybolmuş" gibi görünür.
            map.cameraPosition = CameraPosition.Builder()
                .target(myLocation ?: initialCenter)
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

                // Araç fiyat balonları. Kaynak KÜMELENİR: yakın araçlar düşük zoom'da tek bir "sayı
                // balonu"nda toplanır; zoom arttıkça (clusterRadius piksel sabit) kapsanan coğrafi alan
                // küçülür ve kümeler ayrışır. clusterMaxZoom üstünde her araç tek tek gösterilir.
                loaded.addSource(
                    GeoJsonSource(
                        "vehicles",
                        GeoJsonOptions()
                            .withCluster(true)
                            .withClusterRadius(CLUSTER_RADIUS)
                            .withClusterMaxZoom(CLUSTER_MAX_ZOOM),
                    ),
                )
                // Tekil araçlar (kümelenmemiş): "icon" özelliği ilgili fiyat balonu bitmap'ini seçer.
                loaded.addLayer(
                    SymbolLayer("vehicles-layer", "vehicles").withProperties(
                        PropertyFactory.iconImage("{icon}"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                    ).withFilter(Expression.not(Expression.has("point_count"))),
                )
                // Kümeler: "cluster-<sayı>" ikon adıyla; bitmap talep anında üretilir (aşağıdaki
                // OnStyleImageMissing). Stil glyph/font içermediğinden sayı native text yerine Canvas
                // ile bitmap'e çizilir (fiyat balonlarıyla aynı yaklaşım).
                loaded.addLayer(
                    SymbolLayer("clusters-layer", "vehicles").withProperties(
                        PropertyFactory.iconImage(
                            Expression.concat(
                                Expression.literal(CLUSTER_IMAGE_PREFIX),
                                Expression.toString(Expression.get("point_count")),
                            ),
                        ),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    ).withFilter(Expression.has("point_count")),
                )
                // Küme sayı balonu bitmap'lerini talep anında üret: motor "cluster-<sayı>" ikonunu
                // isteyince (zoom/pan ile sayı değiştikçe) burada üretilip stile eklenir ve önbelleklenir.
                // (OnStyleImageMissing MapLibre'de MapView üzerindedir, MapLibreMap'te değil.)
                mapView.addOnStyleImageMissingListener { id ->
                    if (id.startsWith(CLUSTER_IMAGE_PREFIX)) {
                        loaded.addImage(
                            id,
                            VehicleMarkers.buildCluster(context, id.removePrefix(CLUSTER_IMAGE_PREFIX)),
                        )
                    }
                }

                // Aktif yolculuk araç pin'i (ridePoint) — merkez-çapalı tekil işaretçi.
                // Kaynak home'da boş kalır; yalnız ridePoint verildiğinde doldurulur.
                loaded.addImage("ride-pin", VehicleMarkers.buildRidePin(context))
                loaded.addSource(GeoJsonSource("ride"))
                loaded.addLayer(
                    SymbolLayer("ride-layer", "ride").withProperties(
                        PropertyFactory.iconImage("ride-pin"),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconAnchor(Property.ICON_ANCHOR_CENTER),
                    ),
                )

                // Araç balonuna dokunuş: dokunulan noktanın çevresindeki "vehicles-layer"
                // feature'ları sorgulanır; ilkinin "id"si detay için Screen'e iletilir.
                val slop = 22f * context.resources.displayMetrics.density
                map.addOnMapClickListener { point ->
                    val screen = map.projection.toScreenLocation(point)
                    val rect = RectF(screen.x - slop, screen.y - slop, screen.x + slop, screen.y + slop)
                    // Kümeye dokunma → yakınlaş (küme açılır). clusterMaxZoom'a kadar yaklaştıkça
                    // kümeler bölünür; "zooma bağlı kümeleme"nin etkileşimli karşılığı.
                    val clusterPoint = map.queryRenderedFeatures(rect, "clusters-layer")
                        .firstOrNull()?.geometry() as? Point
                    if (clusterPoint != null) {
                        val target = LatLng(clusterPoint.latitude(), clusterPoint.longitude())
                        val zoom = (map.cameraPosition.zoom + CLUSTER_ZOOM_STEP).coerceAtMost(MAX_CLUSTER_ZOOM)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom))
                        return@addOnMapClickListener true
                    }
                    // Yalnızca müsait araç balonları detay açar; gri "Kullanımda" araçlar
                    // non-owner'a 404 döndüğünden tıklama yok sayılır ("available" == "true").
                    val feature = map.queryRenderedFeatures(rect, "vehicles-layer")
                        .firstOrNull { it.getStringProperty("available") == "true" }
                    val id = feature?.getStringProperty("id")
                    if (id != null) {
                        currentOnVehicleClick(id)
                        true // dokunuş tüketildi
                    } else {
                        false
                    }
                }

                mapAndStyle = map to loaded
            }
        }
    }

    // Konum noktası -> myLocation her değiştiğinde güncellenir, kamera oynamaz.
    LaunchedEffect(mapAndStyle, myLocation) {
        val (_, style) = mapAndStyle ?: return@LaunchedEffect
        updateMe(style, myLocation)
    }

    // Araç balonları -> vehicles listesi her değiştiğinde bitmap'ler eklenir/temizlenir.
    val addedIconIds = remember { mutableSetOf<String>() }
    LaunchedEffect(mapAndStyle, vehicles, recommendedVehicleIds) {
        val (_, style) = mapAndStyle ?: return@LaunchedEffect
        // Sadece müsait olanları VE (eğer AI varsa) AI önerisi olanları filtreleyerek gönder
        val filteredVehicles = if (recommendedVehicleIds.isNotEmpty()) {
            vehicles.filter { it.id in recommendedVehicleIds }
        } else {
            vehicles
        }
        updateVehicles(context, style, filteredVehicles, recommendedVehicleIds, addedIconIds)
    }

    // Aktif yolculuk pin'i -> ridePoint her değiştiğinde işaretçi güncellenir ve kamera onu takip
    // eder. İlk konumda araca yakınlaşılır (RIDE_ZOOM); sonrasında zoom korunarak kaydırılır
    // (kullanıcının yaptığı yakınlaştırmayla çakışmamak için).
    var hasCenteredRide by remember { mutableStateOf(false) }
    LaunchedEffect(mapAndStyle, ridePoint) {
        val (map, style) = mapAndStyle ?: return@LaunchedEffect
        updateRide(style, ridePoint)
        if (ridePoint != null) {
            if (!hasCenteredRide) {
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(ridePoint, RIDE_ZOOM))
                hasCenteredRide = true
            } else {
                map.animateCamera(CameraUpdateFactory.newLatLng(ridePoint))
            }
        }
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

/** "ride" GeoJSON kaynağını aktif yolculuk araç konumuna göre günceller; yoksa pin'i gizler. */
private fun updateRide(style: Style, point: LatLng?) {
    val source = style.getSourceAs<GeoJsonSource>("ride") ?: return
    if (point == null) {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        source.setGeoJson(Point.fromLngLat(point.longitude, point.latitude))
    }
}

/**
 * "vehicles" kaynağını araç listesine göre günceller. Her araç için fiyat balonu bitmap'i
 * stile bir kez eklenir ("veh-<id>"); listeden düşen araçların ikonları temizlenir.
 * Her feature, kendi ikonunu seçmesi için "icon" string özelliğini taşır.
 */
private fun updateVehicles(
    context: Context,
    style: Style,
    vehicles: List<VehicleUi>,
    recommendedVehicleIds: Set<String>,
    addedIconIds: MutableSet<String>,
) {
    val source = style.getSourceAs<GeoJsonSource>("vehicles") ?: return

    // İkon kimliği içeriğe göre imzalanır (id + status + recommended): araç kullanımdan müsaite geçince
    // (veya tersi) veya önerilirse bitmap yeniden üretilmelidir.
    fun iconIdOf(v: VehicleUi) = "veh-${v.id}-${v.status}-${v.id in recommendedVehicleIds}"

    val currentIds = vehicles.mapTo(mutableSetOf()) { iconIdOf(it) }
    // Artık listede olmayan (veya durumu değişen) araçların ikonlarını stilden kaldır.
    val stale = addedIconIds - currentIds
    stale.forEach { style.removeImage(it) }
    addedIconIds.removeAll(stale)

    val features = vehicles.map { vehicle ->
        val available = VehicleMarkers.isAvailable(vehicle.status)
        val isRecommended = vehicle.id in recommendedVehicleIds
        val iconId = iconIdOf(vehicle)
        if (addedIconIds.add(iconId)) {
            val baseColor = VehicleMarkers.colorFor(vehicle.segment, vehicle.type, vehicle.status)
            val bitmap = VehicleMarkers.build(
                context = context,
                label = VehicleMarkers.labelFor(vehicle.status, vehicle.pricePerDay),
                backgroundColor = if (isRecommended) VehicleMarkers.CAT_AI else baseColor,
                glow = available || isRecommended,
            )
            style.addImage(iconId, bitmap)
        }
        Feature.fromGeometry(
            Point.fromLngLat(vehicle.longitude, vehicle.latitude),
        ).apply {
            addStringProperty("icon", iconId)
            // Dokunuşta detay ekranı için araç kimliği (queryRenderedFeatures ile okunur).
            addStringProperty("id", vehicle.id)
            // Yalnızca müsait araçlar tıklanabilir (gri "Kullanımda" araçlar detay açmaz).
            addStringProperty("available", if (available) "true" else "false")
        }
    }

    source.setGeoJson(FeatureCollection.fromFeatures(features))
}
