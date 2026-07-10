package com.turkcell.rencar.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.RenCarTheme
import org.maplibre.android.geometry.LatLng

/** İnce + kaba konum izinleri; ikisinden biri yeterlidir. */
private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

// ── Stateful sarmalayıcı (§4.5): framework mekaniğini yönetir, durumu VM'e intent'ler ──
@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
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

    // İzin verildiğinde konum güncellemelerini dinle; izin/ekran değişince temizle.
    DisposableEffect(uiState.hasLocationPermission) {
        if (!uiState.hasLocationPermission) return@DisposableEffect onDispose { }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    viewModel.onIntent(MapIntent.LocationChanged(LatLng(loc.latitude, loc.longitude)))
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
        mapController.animateTo(location)
        viewModel.onIntent(MapIntent.CenteredOnUser)
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
                            mapController.animateTo(target)
                        }
                    } else {
                        permissionLauncher.launch(LOCATION_PERMISSIONS)
                    }
                }

                else -> viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer; `controller` harita görünüm tutamacıdır ──
@Composable
private fun MapScreen(
    uiState: MapUiState,
    controller: RencarMapController,
    onIntent: (MapIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        RencarMap(
            myLocation = uiState.myLocation,
            modifier = Modifier.fillMaxSize(),
            controller = controller,
        )

        // Sağ alt -> konumu yeniden al ve kamerayı tekrar zoomla.
        FloatingActionButton(
            onClick = { onIntent(MapIntent.RecenterClicked) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            Icon(RencarIcons.MyLocation, contentDescription = "Konumuma git")
        }
    }
}

/** İnce ya da kaba konum izninden en az biri verilmiş mi. */
private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Cache'lenmiş son konum yerine taze, tek seferlik yüksek doğruluklu konum ister. */
@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    fusedClient: FusedLocationProviderClient,
    onLocation: (LatLng) -> Unit,
) {
    fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
        .addOnSuccessListener { location ->
            if (location != null) onLocation(LatLng(location.latitude, location.longitude))
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
            uiState = MapUiState(),
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
            uiState = MapUiState(),
            controller = rememberRencarMapController(),
            onIntent = {},
        )
    }
}
