package com.turkcell.rencar.ui.reservation

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.QuoteUi
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.util.Locale

/** Marka mavisi — her iki temada AYNI (colorScheme.primary koyu temada pastele döner). */
private val RencarBlue = LightPrimary

/** Türkçe biçimlendirme (virgüllü ondalık, noktalı binlik). */
private val TrLocale = Locale.forLanguageTag("tr")

/** Rezervasyon serbest tutma süresi — API'de rezervasyon öncesi sorgulanamaz (RESERVATION_TTL_MIN, varsayılan 15). */
private const val FreeReservationLabel = "15 dk"

// ── Stateful sarmalayıcı (§4.5): vehicleId ViewModel'e SavedStateHandle ile gelir ──
@Composable
fun ReservationScreen(
    onNavigateBack: () -> Unit,
    onReserved: (vehicleId: String, plan: RentalPlan) -> Unit,
    viewModel: ReservationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // POST /reservations başarılı → kısa bildirim + sonraki adıma geç (plan'a göre foto/Home); bayrağı tüket.
    LaunchedEffect(uiState.reserved) {
        if (uiState.reserved) {
            Toast.makeText(context, "Araç $FreeReservationLabel için rezerve edildi.", Toast.LENGTH_SHORT).show()
            onReserved(uiState.vehicle?.id.orEmpty(), uiState.selectedPlan)
            viewModel.onReservedHandled()
        }
    }

    ReservationScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        onBack = onNavigateBack,
    )
}

// ── Stateless gövde (§4.5): tam ekran; üstte başlık, altta sabit tamamla butonu ──
@Composable
private fun ReservationScreen(
    uiState: ReservationUiState,
    onIntent: (ReservationIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        TopBar(onBack = onBack)

        val vehicle = uiState.vehicle
        when {
            vehicle == null && uiState.isLoading -> LoadingState(Modifier.weight(1f))
            vehicle == null && uiState.errorMessage != null ->
                ErrorState(
                    message = uiState.errorMessage,
                    onRetry = { onIntent(ReservationIntent.Retry) },
                    modifier = Modifier.weight(1f),
                )

            vehicle != null -> {
                // Kaydırılabilir içerik (kart + plan + döküm + şartlar).
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 4.dp, bottom = 16.dp),
                ) {
                    VehicleCard(vehicle)
                    Spacer(Modifier.height(16.dp))
                    PlanCard(
                        vehicle = vehicle,
                        selectedPlan = uiState.selectedPlan,
                        onPlanSelected = { onIntent(ReservationIntent.PlanSelected(it)) },
                    )
                    Spacer(Modifier.height(16.dp))
                    BreakdownCard(
                        quote = uiState.quote,
                        estimateLabel = uiState.selectedPlan.estimateLabel,
                        isQuoteLoading = uiState.isQuoteLoading,
                    )
                    Spacer(Modifier.height(16.dp))
                    TermsRow(
                        checked = uiState.termsAccepted,
                        onToggle = { onIntent(ReservationIntent.TermsToggled) },
                    )
                    // Rezervasyon hatası (409 vb.) döküm altında gösterilir.
                    uiState.errorMessage?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                BottomBar(
                    canReserve = uiState.canReserve,
                    isReserving = uiState.isReserving,
                    onReserve = { onIntent(ReservationIntent.ReserveClicked) },
                )
            }

            else -> Spacer(Modifier.weight(1f))
        }
    }
}

// ── Üst başlık: ‹ geri + "Rezervasyon Onayı" ──
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .padding(top = 32.dp), // durum çubuğu payı (systemBars yaklaşımı diğer ekranlarla uyumlu)
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.ChevronLeft,
                contentDescription = "Geri",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Rezervasyon Onayı",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Araç kartı: küçük görsel + marka/model, plaka·vites·koltuk, yakıt rozeti ──
@Composable
private fun VehicleCard(vehicle: VehicleUi) {
    Card {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = categoryColor(vehicle.type)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(tint.copy(alpha = 0.20f), tint.copy(alpha = 0.06f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Car,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${vehicle.brand} ${vehicle.model}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = specLine(vehicle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                vehicle.fuelPercent?.let {
                    Spacer(Modifier.height(8.dp))
                    FuelBadge(percent = it)
                }
            }
        }
    }
}

// ── Yakıt rozeti: yeşil pill "Yakıt %72" ──
@Composable
private fun FuelBadge(percent: Double) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.rencar.successContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = RencarIcons.Fuel,
            contentDescription = null,
            tint = MaterialTheme.rencar.onSuccessContainer,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Yakıt %${percent.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.rencar.onSuccessContainer,
        )
    }
}

// ── Kiralama planı: 3 çip (Dakikalık/Saatlik/Günlük) ──
@Composable
private fun PlanCard(
    vehicle: VehicleUi,
    selectedPlan: RentalPlan,
    onPlanSelected: (RentalPlan) -> Unit,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kiralama planı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RentalPlan.entries.forEach { plan ->
                    PlanChip(
                        label = plan.label,
                        price = planPrice(vehicle, plan),
                        selected = plan == selectedPlan,
                        onClick = { onPlanSelected(plan) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanChip(
    label: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) RencarBlue else MaterialTheme.colorScheme.outlineVariant
    val bg = if (selected) RencarBlue.copy(alpha = 0.08f) else Color.Transparent
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(
                BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) RencarBlue else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = price,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) RencarBlue else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Ücret dökümü kartı ──
@Composable
private fun BreakdownCard(
    quote: QuoteUi?,
    estimateLabel: String,
    isQuoteLoading: Boolean,
) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            BreakdownRow(
                label = "Ücretsiz rezervasyon",
                value = FreeReservationLabel,
            )
            Spacer(Modifier.height(10.dp))
            BreakdownRow(
                label = "Başlangıç ücreti",
                value = amountOrDash(quote?.startFee, isQuoteLoading, decimals = 2),
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))
            BreakdownRow(
                label = "Tahmini ücret ($estimateLabel)",
                value = quote?.usageFee?.let { "~" + formatTl(it, decimals = 0) }
                    ?: if (isQuoteLoading) "…" else "—",
                emphasize = true,
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Kullanım şartları onay satırı ──
@Composable
private fun TermsRow(checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) RencarBlue else MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = RencarIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Kullanım şartlarını ve kasko/sigorta koşullarını okudum, onaylıyorum.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Alt sabit buton: "Rezervasyonu Tamamla" ──
@Composable
private fun BottomBar(
    canReserve: Boolean,
    isReserving: Boolean,
    onReserve: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 24.dp),
    ) {
        Button(
            onClick = onReserve,
            enabled = canReserve,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RencarBlue,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            if (isReserving) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(text = "Rezervasyonu Tamamla", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Ortak kart kabı ──
@Composable
private fun Card(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        content()
    }
}

// ── Yükleniyor / Hata ──
@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
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

/** "34 RNC 022 · Manuel · 5 kişi" — eksik alanlar atlanır. */
private fun specLine(vehicle: VehicleUi): String = buildList {
    add(vehicle.plate)
    vehicle.transmission?.let { add(transmissionText(it)) }
    vehicle.seats?.let { add("$it kişi") }
}.joinToString(" · ")

private fun transmissionText(transmission: String): String = when (transmission.uppercase()) {
    "MANUAL" -> "Manuel"
    "AUTOMATIC" -> "Otomatik"
    else -> transmission
}

/** Plan çipinin fiyat satırı; ilgili fiyat null (canlı sunucu) ise "—". */
private fun planPrice(vehicle: VehicleUi, plan: RentalPlan): String = when (plan) {
    RentalPlan.PER_MINUTE -> vehicle.pricePerMinute?.let { "${formatTl(it, 2)}/dk" } ?: "—"
    RentalPlan.HOURLY -> vehicle.pricePerHour?.let { "${formatTl(it, 0)}/sa" } ?: "—"
    RentalPlan.DAILY -> formatTl(vehicle.pricePerDay, 0)
}

/** Tutarı biçimler; yoksa yükleniyorsa "…", değilse "—". */
private fun amountOrDash(amount: Double?, isLoading: Boolean, decimals: Int): String =
    amount?.let { formatTl(it, decimals) } ?: if (isLoading) "…" else "—"

/** Türk Lirası biçimi: "₺4,50" (dk), "₺180" (saat), "₺1.450" (gün). */
private fun formatTl(amount: Double, decimals: Int): String =
    "₺" + String.format(TrLocale, "%,.${decimals}f", amount)

/** Araç tipi → kategori rengi (VehicleDetail/VehicleMarkers ile aynı eşleme). */
@Composable
private fun categoryColor(type: String): Color = when (type.uppercase()) {
    "SUV" -> MaterialTheme.rencar.catSuv
    "HATCHBACK" -> MaterialTheme.rencar.catEconomy
    "STATION" -> MaterialTheme.rencar.catComfort
    "MINIVAN" -> MaterialTheme.rencar.catElectric
    else -> RencarBlue
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewVehicle = VehicleUi(
    id = "veh-1",
    plate = "34 RNC 022",
    brand = "Renault",
    model = "Clio",
    type = "HATCHBACK",
    pricePerDay = 1450.0,
    status = "AVAILABLE",
    latitude = 41.0151,
    longitude = 28.9795,
    pricePerMinute = 4.5,
    pricePerHour = 180.0,
    fuelPercent = 72.0,
    rangeKm = 480.0,
    transmission = "MANUAL",
    seats = 5,
    segment = "ECONOMY",
)

private val PreviewQuote = QuoteUi(
    vehicleId = "veh-1",
    plan = "PER_MINUTE",
    minutes = 30,
    usageFee = 135.0,
    startFee = 15.0,
    serviceFee = 7.0,
    estimatedTotal = 157.0,
)

@Preview(name = "Reservation · Light", showBackground = true, heightDp = 820)
@Composable
private fun ReservationLightPreview() {
    RenCarTheme(darkTheme = false) {
        ReservationScreen(
            uiState = ReservationUiState(vehicle = PreviewVehicle, quote = PreviewQuote, termsAccepted = true),
            onIntent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Reservation · Dark", showBackground = true, heightDp = 820)
@Composable
private fun ReservationDarkPreview() {
    RenCarTheme(darkTheme = true) {
        ReservationScreen(
            uiState = ReservationUiState(vehicle = PreviewVehicle, quote = PreviewQuote, termsAccepted = false),
            onIntent = {},
            onBack = {},
        )
    }
}
