package com.turkcell.rencar.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.turkcell.rencar.data.model.GeoPoint
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.vehicledetail.VehicleDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.geometry.LatLng

/** İnce + kaba konum izinleri; ikisinden biri yeterlidir. */
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** "En Yakın Aracı Bul" ile en yakın araca gidince kullanılan yakınlaştırma seviyesi. */
private const val NEAREST_ZOOM = 15.0

// ── Stateful sarmalayıcı (§4.5): framework mekaniğini yönetir, durumu VM'e intent'ler ──
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    onNavigateToReservation: (vehicleId: String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mapController = rememberRencarMapController()
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onIntent(MapIntent.PermissionResult(granted))
    }

    // İlk açılış: izin zaten varsa state'e yaz, yoksa sistem diyaloğunu aç.
    LaunchedEffect(Unit) {
        if (context.hasLocationPermission()) {
            viewModel.onIntent(MapIntent.PermissionResult(true))
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // İlk açılış: araçları API'den yükle (konum iznine bağlı değildir).
    LaunchedEffect(Unit) {
        viewModel.onIntent(MapIntent.LoadVehicles)
    }

    // İzin verildiğinde konum güncellemelerini dinle; izin/ekran değişince temizle.
    DisposableEffect(uiState.hasLocationPermission) {
        if (!uiState.hasLocationPermission) return@DisposableEffect onDispose { }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    viewModel.onIntent(MapIntent.LocationChanged(GeoPoint(loc.latitude, loc.longitude)))
                }
            }
        }
        startLocationUpdates(fusedClient, callback)

        onDispose { fusedClient.removeLocationUpdates(callback) }
    }

    // İlk konum geldiğinde tek seferlik kamera zoom'u (sonraki güncellemeler kamerayı oynatmaz).
    LaunchedEffect(uiState.myLocation, uiState.hasCenteredOnUser) {
        val location = uiState.myLocation ?: return@LaunchedEffect
        if (uiState.hasCenteredOnUser) return@LaunchedEffect
        mapController.animateTo(location.toLatLng())
        viewModel.onIntent(MapIntent.CenteredOnUser)
    }

    // Konum çözülünce mahalle adını cihaz Geocoder'ıyla bir kez çöz (API dışı, decisions.md).
    // Alt kart altyazısındaki "… çevresinde" bilgisini besler; başarısızsa sessizce atlanır.
    LaunchedEffect(uiState.myLocation) {
        val location = uiState.myLocation ?: return@LaunchedEffect
        if (uiState.localityName != null) return@LaunchedEffect
        val name = withContext(Dispatchers.IO) { reverseGeocodeLocality(context, location) }
        viewModel.onIntent(MapIntent.LocalityResolved(name))
    }

    MapScreen(
        uiState = uiState,
        controller = mapController,
        onIntent = { intent ->
            when (intent) {
                // Konum yenile + kamerayı ortala: taze konum iste, gelince VM'e yaz ve kamerayı taşı.
                MapIntent.RecenterClicked -> {
                    if (uiState.hasLocationPermission) {
                        fetchCurrentLocation(fusedClient) { target ->
                            viewModel.onIntent(MapIntent.LocationChanged(target))
                            mapController.animateTo(target.toLatLng())
                        }
                    } else {
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                }

                // Zoom saf görsel iş: controller kamerayı bir kademe oynatır (VM'e gitmez).
                MapIntent.ZoomIn -> mapController.zoomIn()
                MapIntent.ZoomOut -> mapController.zoomOut()

                // En yakın müsait araca kamerayı taşı ve detay alt sayfasını aç.
                MapIntent.FindNearest -> {
                    uiState.nearestVehicle?.let { v ->
                        mapController.animateTo(LatLng(v.latitude, v.longitude), NEAREST_ZOOM)
                        viewModel.onIntent(MapIntent.VehicleClicked(v.id))
                    }
                }

                MapIntent.AiClicked -> {
                    if (uiState.recommendedVehicleIds.isNotEmpty()) {
                        viewModel.onIntent(MapIntent.ClearAiRecommendations)
                    } else {
                        viewModel.onIntent(MapIntent.AiClicked)
                    }
                }

                else -> viewModel.onIntent(intent)
            }
        },
        onNavigateToReservation = onNavigateToReservation,
        modifier = modifier,
    )

    // AI Önerisi Diyaloğu
    if (uiState.showAiDialog) {
        AiRecommendationDialog(
            vehicles = uiState.vehicles,
            onDismiss = { viewModel.onIntent(MapIntent.AiDismissed) },
            onApply = { ids -> viewModel.onIntent(MapIntent.SetAiRecommendations(ids)) },
        )
    }
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer; `controller` harita görünüm tutamacıdır ──
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapScreen(
    uiState: MapUiState,
    controller: RencarMapController,
    onIntent: (MapIntent) -> Unit,
    onNavigateToReservation: (vehicleId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        RencarMap(
            myLocation = uiState.myLocation?.toLatLng(),
            modifier = Modifier.fillMaxSize(),
            controller = controller,
            vehicles = uiState.vehicles,
            recommendedVehicleIds = uiState.recommendedVehicleIds,
            onVehicleClick = { id -> onIntent(MapIntent.VehicleClicked(id)) },
        )

        // Üst -> araç yükleme hatası (varsa); sessiz kalmasın diye görünür banner + tekrar dene.
        if (uiState.vehiclesError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.vehiclesError,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Tekrar dene",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onIntent(MapIntent.LoadVehicles) },
                    )
                }
            }
        }

        // Alt -> zoom/konum kontrolleri (sağa yaslı) ve hemen altında kalıcı bilgi kartı.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SmallFloatingActionButton(
                    onClick = { onIntent(MapIntent.ZoomIn) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(text = "+", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = { onIntent(MapIntent.ZoomOut) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text(text = "−", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { onIntent(MapIntent.RecenterClicked) },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                ) {
                    Icon(RencarIcons.MyLocation, contentDescription = "Konumuma git")
                }
            }

            MapBottomCard(
                availableCount = uiState.availableCount,
                localityName = uiState.localityName,
                nearestDistanceMeters = uiState.nearestDistanceMeters,
                selectedSegment = uiState.selectedSegment,
                recommendedCount = uiState.recommendedVehicleIds.size,
                expanded = uiState.bottomCardExpanded,
                onToggle = { onIntent(MapIntent.ToggleBottomCard) },
                onSegmentSelected = { onIntent(MapIntent.SegmentSelected(it)) },
                onFindNearest = { onIntent(MapIntent.FindNearest) },
                onAiClick = { onIntent(MapIntent.AiClicked) },
            )
        }

        // Araca dokununca detay alt sayfası (bottom sheet) açılır; veri GET /vehicles/{id}'den gelir.
        // Uzaklık için anlık kullanıcı konumu iletilir (yoksa uzaklık satırı gizlenir).
        val selectedVehicleId = uiState.selectedVehicleId
        if (selectedVehicleId != null) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { onIntent(MapIntent.VehicleDismissed) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                VehicleDetailScreen(
                    vehicleId = selectedVehicleId,
                    userLatitude = uiState.myLocation?.latitude,
                    userLongitude = uiState.myLocation?.longitude,
                    // "Rezerve Et" → alt sayfayı kapat ve rezervasyon onayına git.
                    onReserve = {
                        onIntent(MapIntent.VehicleDismissed)
                        onNavigateToReservation(selectedVehicleId)
                    },
                )
            }
        }
    }
}


/** UI/domain konum modelini harita motoru tipine çevirir (yalnız harita sınırında kullanılır). */
private fun GeoPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

/** İnce ya da kaba konum izninden en az biri verilmiş mi. */
private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Konumu cihaz Geocoder'ıyla bir mahalle/semt adına çevirir (API DIŞI). Geocoder bloklayıcıdır;
 * çağıran IO dispatcher'ında yürütür. Servis yoksa veya hata olursa null döner (altyazı gizlenir).
 */
@Suppress("DEPRECATION")
private fun reverseGeocodeLocality(context: Context, location: GeoPoint): String? {
    if (!Geocoder.isPresent()) return null
    return runCatching {
        Geocoder(context)
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?.let { it.subLocality ?: it.locality ?: it.subAdminArea }
    }.getOrNull()
}

/** Cache'lenmiş son konum yerine taze, tek seferlik yüksek doğruluklu konum ister. */
@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    fusedClient: FusedLocationProviderClient,
    onLocation: (GeoPoint) -> Unit,
) {
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { location ->
            if (location != null) onLocation(GeoPoint(location.latitude, location.longitude))
        }
}

/** 5sn (min 2sn) aralıklarla yüksek doğruluklu konum güncellemelerini başlatır. */
@SuppressLint("MissingPermission")
private fun startLocationUpdates(
    fusedClient: FusedLocationProviderClient,
    callback: LocationCallback,
) {
    val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
        .setMinUpdateIntervalMillis(2_000L)
        .build()

    // İlk açılışta hemen bir konum ver (cache'li son konum), ardından periyodik güncellemeler.
    fusedClient.lastLocation.addOnSuccessListener { location ->
        if (location != null) {
            callback.onLocationResult(LocationResult.create(listOf(location)))
        }
    }

    fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5). RencarMap önizlemede placeholder. ──
@Preview(name = "Map · Light", showBackground = true)
@Composable
private fun MapScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        MapScreen(
            uiState = MapUiState(availableCount = 12),
            controller = rememberRencarMapController(),
            onIntent = {},
        )
    }
}

@Preview(name = "Map · Dark", showBackground = true)
@Composable
private fun MapScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        MapScreen(
            uiState = MapUiState(availableCount = 12),
            controller = rememberRencarMapController(),
            onIntent = {},
        )
    }
}
