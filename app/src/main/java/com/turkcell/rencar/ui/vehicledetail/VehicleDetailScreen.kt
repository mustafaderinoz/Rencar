package com.turkcell.rencar.ui.vehicledetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.remote.dto.VehicleResponse
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Marka mavisi — buton/border her iki temada AYNI mavidir (colorScheme.primary koyu temada
 * pastele döner). Tema-bağımsız marka token'ı [LightPrimary] kullanılır (bkz. Login/Onboarding).
 */
private val RencarBlue = LightPrimary

/** Türkçe biçimlendirme (virgüllü ondalık, noktalı binlik). */
private val TrLocale = Locale.forLanguageTag("tr")

// ── Stateful sarmalayıcı (§4.5): id + kullanıcı konumunu alır, VM'i besler ──
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    userLatitude: Double?,
    userLongitude: Double?,
    modifier: Modifier = Modifier,
    onReserve: () -> Unit = {},
    onUnlock: () -> Unit = {},
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Açılışta (veya farklı araç seçilince) GET /vehicles/{id} tetiklenir; uzaklık için konum iletilir.
    LaunchedEffect(vehicleId, userLatitude, userLongitude) {
        viewModel.onIntent(VehicleDetailIntent.Load(vehicleId, userLatitude, userLongitude))
    }

    VehicleDetailScreen(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                VehicleDetailIntent.ReserveClicked -> { viewModel.onIntent(intent); onReserve() }
                VehicleDetailIntent.UnlockClicked -> { viewModel.onIntent(intent); onUnlock() }
                else -> viewModel.onIntent(intent)
            }
        },
        modifier = modifier,
    )
}

// ── Stateless gövde (§4.5): alt sayfa (bottom sheet) içeriği ──
@Composable
private fun VehicleDetailScreen(
    uiState: VehicleDetailUiState,
    onIntent: (VehicleDetailIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        val vehicle = uiState.vehicle
        when {
            vehicle == null && uiState.isLoading -> LoadingState()
            vehicle == null && uiState.errorMessage != null ->
                ErrorState(message = uiState.errorMessage, onRetry = { onIntent(VehicleDetailIntent.Retry) })

            vehicle != null -> VehicleContent(
                vehicle = vehicle,
                distanceMeters = uiState.distanceMeters,
                isAvailable = uiState.isAvailable,
                onIntent = onIntent,
            )

            else -> Spacer(Modifier.height(120.dp))
        }
    }
}

// ── İçerik ──
@Composable
private fun VehicleContent(
    vehicle: VehicleResponse,
    distanceMeters: Float?,
    isAvailable: Boolean,
    onIntent: (VehicleDetailIntent) -> Unit,
) {
    // Başlık: marka/model + durum rozeti, altta plaka · uzaklık.
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${vehicle.brand} ${vehicle.model}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitleText(vehicle.plate, distanceMeters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        StatusBadge(status = vehicle.status)
    }

    Spacer(Modifier.height(16.dp))

    // Araç görseli — API'de görsel alanı yok; tipe göre yerel illüstrasyon (tema uyumlu).
    // İleride gerçek PNG'ler res/drawable'a eklenip type ile eşlenerek bu Box değiştirilebilir.
    VehicleImage(type = vehicle.type)

    Spacer(Modifier.height(16.dp))

    // 2×2 bilgi ızgarası: Yakıt | Menzil / Vites | Koltuk (tümü API'den).
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            icon = RencarIcons.Fuel,
            label = "Yakıt",
            value = vehicle.fuelPercent?.let { "%${it.roundToInt()}" } ?: "—",
            modifier = Modifier.weight(1f),
        ) {
            // Bar yalnız API değeri geldiğinde çizilir (yoksa alan boş kalır).
            vehicle.fuelPercent?.let {
                Spacer(Modifier.height(8.dp))
                FuelBar(percent = it)
            }
        }
        InfoCard(
            icon = RencarIcons.MapPin,
            label = "Menzil",
            value = vehicle.rangeKm?.let { "~${it.roundToInt()} km" } ?: "—",
            modifier = Modifier.weight(1f),
        ) {
            if (vehicle.rangeKm != null) {
                Text(
                    text = "Tahmini menzil",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            icon = RencarIcons.Gear,
            label = "Vites",
            value = vehicle.transmission?.let { transmissionText(it) } ?: "—",
            modifier = Modifier.weight(1f),
        )
        InfoCard(
            icon = RencarIcons.Seat,
            label = "Koltuk",
            value = vehicle.seats?.let { "$it kişi" } ?: "—",
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(16.dp))

    // Fiyat satırı — dakikalık varsa öne çıkar; yoksa (canlı sunucu) günlük fiyata düşer.
    // pricePerDay her zaman gelir; pricePerMinute/pricePerHour backend deploy'una kadar null olabilir.
    val perMinute = vehicle.pricePerMinute
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = if (perMinute != null) formatTl(perMinute, decimals = 2) else formatTl(vehicle.pricePerDay, decimals = 0),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (perMinute != null) "/dk" else "/gün",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            vehicle.pricePerHour?.let {
                Text(
                    text = "Saatlik ${formatTl(it, decimals = 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            // Günlük'ü yalnız sol tarafta /dk gösterilirken ekle (aksi halde tekrar eder).
            if (perMinute != null) {
                Text(
                    text = "Günlük ${formatTl(vehicle.pricePerDay, decimals = 0)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    Spacer(Modifier.height(20.dp))

    // Butonlar — kural: araç kiralanmamış (AVAILABLE) ise Rezerve Et aktif, Kilidi Aç pasif.
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = { onIntent(VehicleDetailIntent.ReserveClicked) },
            enabled = isAvailable,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RencarBlue),
            border = BorderStroke(
                width = 1.dp,
                color = if (isAvailable) RencarBlue else MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Text(text = "Rezerve Et", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = { onIntent(VehicleDetailIntent.UnlockClicked) },
            // Yalnız rezerve/kiralanmış araçta anlamlı; müsait araçta (kiralanmamış) pasif.
            enabled = !isAvailable,
            modifier = Modifier
                .weight(1f)
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RencarBlue,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Icon(
                imageVector = RencarIcons.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "Kilidi Aç", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Durum rozeti: MÜSAİT (yeşil) / REZERVE / DOLU / BAKIMDA (nötr) ──
@Composable
private fun StatusBadge(status: String) {
    val (bg, fg, label) = when (status) {
        "AVAILABLE" -> Triple(MaterialTheme.rencar.successContainer, MaterialTheme.rencar.onSuccessContainer, "MÜSAİT")
        "RESERVED" -> Triple(MaterialTheme.rencar.warningContainer, MaterialTheme.rencar.onWarningContainer, "REZERVE")
        "RENTED" -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, "DOLU")
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, "BAKIMDA")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = fg,
        )
    }
}

// ── Tipe göre yerel araç illüstrasyonu (görsel API'de yok) ──
@Composable
private fun VehicleImage(type: String, modifier: Modifier = Modifier) {
    val tint = categoryColor(type)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.06f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = RencarIcons.Car,
            contentDescription = "Araç görseli",
            tint = tint,
            modifier = Modifier.size(96.dp),
        )
    }
}

// ── Bilgi kartı: üstte ikon+etiket, altta değer (+ opsiyonel accent: bar/alt yazı) ──
@Composable
private fun InfoCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            accent()
        }
    }
}

// ── Yakıt barı (yeşil = success token) ──
@Composable
private fun FuelBar(percent: Double) {
    val fraction = (percent / 100.0).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.rencar.success),
        )
    }
}

// ── Yükleniyor / Hata durumları ──
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(text = "Tekrar dene", color = RencarBlue)
        }
    }
}

// ── Yardımcılar ──

/** "34 RNC 022 · 250 m uzaklıkta" — konum yoksa yalnız plaka. */
private fun subtitleText(plate: String, distanceMeters: Float?): String {
    val distance = distanceMeters ?: return plate
    val meters = distance.roundToInt()
    val label = if (meters < 1000) {
        "$meters m uzaklıkta"
    } else {
        String.format(TrLocale, "%.1f km uzaklıkta", distance / 1000f)
    }
    return "$plate · $label"
}

private fun transmissionText(transmission: String): String = when (transmission.uppercase()) {
    "MANUAL" -> "Manuel"
    "AUTOMATIC" -> "Otomatik"
    else -> transmission
}

/** Türk Lirası biçimi: "₺4,50" (dk), "₺180" (saat), "₺1.500" (gün). */
private fun formatTl(amount: Double, decimals: Int): String =
    "₺" + String.format(TrLocale, "%,.${decimals}f", amount)

/** Araç tipi → kategori rengi (VehicleMarkers ile aynı marka eşlemesi, tema token'ları). */
@Composable
private fun categoryColor(type: String): Color = when (type.uppercase()) {
    "SUV" -> MaterialTheme.rencar.catSuv
    "HATCHBACK" -> MaterialTheme.rencar.catEconomy
    "STATION" -> MaterialTheme.rencar.catComfort
    "MINIVAN" -> MaterialTheme.rencar.catElectric
    else -> RencarBlue // SEDAN + varsayılan
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewVehicle = VehicleResponse(
    id = "veh-1",
    plate = "34 RNC 022",
    brand = "Renault",
    model = "Clio",
    type = "HATCHBACK",
    pricePerDay = 1800.0,
    pricePerMinute = 4.5,
    pricePerHour = 180.0,
    fuelPercent = 72.0,
    rangeKm = 480.0,
    transmission = "MANUAL",
    seats = 5,
    segment = "ECONOMY",
    status = "AVAILABLE",
    latitude = 41.0151,
    longitude = 28.9795,
    createdAt = "2026-06-30T10:00:00.000Z",
    updatedAt = "2026-06-30T10:00:00.000Z",
)

@Preview(name = "VehicleDetail · Light", showBackground = true)
@Composable
private fun VehicleDetailLightPreview() {
    RenCarTheme(darkTheme = false) {
        VehicleDetailScreen(
            uiState = VehicleDetailUiState(vehicle = PreviewVehicle, distanceMeters = 250f),
            onIntent = {},
        )
    }
}

@Preview(name = "VehicleDetail · Dark", showBackground = true)
@Composable
private fun VehicleDetailDarkPreview() {
    RenCarTheme(darkTheme = true) {
        VehicleDetailScreen(
            uiState = VehicleDetailUiState(vehicle = PreviewVehicle, distanceMeters = 250f),
            onIntent = {},
        )
    }
}
