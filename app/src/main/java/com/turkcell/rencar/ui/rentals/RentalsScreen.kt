package com.turkcell.rencar.ui.rentals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.RentalHistoryItemUi
import com.turkcell.rencar.data.model.RentalStatsUi
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar

// Rota çizgisi/uç noktası — tema-bağımsız marka mavisi (bkz. RencarBottomBar; memory: brand-blue token).
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5): state'i toplar, sekmeye her girişte tazeler ──
@Composable
fun RentalsScreen(viewModel: RentalsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Ekran her göründüğünde (sekmeye dönüşte) yeniden yükler → yeni kiralamalar listede belirir.
    LaunchedEffect(Unit) { viewModel.onIntent(RentalsIntent.Load) }
    RentalsScreen(uiState = uiState, onIntent = viewModel::onIntent)
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer ──
@Composable
private fun RentalsScreen(
    uiState: RentalsUiState,
    onIntent: (RentalsIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            uiState.isLoading ->
                LoadingState()

            uiState.loadError != null && uiState.rentals.isEmpty() ->
                ErrorState(message = uiState.loadError, onRetry = { onIntent(RentalsIntent.Retry) })

            else ->
                Content(uiState = uiState)
        }
    }
}

@Composable
private fun Content(uiState: RentalsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") { Header(stats = uiState.stats) }

        if (uiState.isEmpty) {
            item(key = "empty") { EmptyState() }
        } else {
            items(uiState.rentals, key = { it.id }) { rental ->
                RentalCard(rental = rental)
            }
        }
    }
}

// ── Başlık: "Kiralamalarım" + "Bu ay N yolculuk · ₺X harcama" (GET /rentals/stats) ──
@Composable
private fun Header(stats: RentalStatsUi?) {
    Column {
        Text(
            text = "Kiralamalarım",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (stats != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stats.summaryLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Tek kiralama kartı: rota küçük görseli + araç/tarih/rozetler + tutar/durum ──
@Composable
private fun RentalCard(rental: RentalHistoryItemUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RouteThumbnail(modifier = Modifier.size(64.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = rental.vehicleTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                TrailingAmount(rental = rental)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = rental.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricPill(text = rental.durationLabel)
                MetricPill(text = rental.distanceLabel)
            }
        }
    }
}

/** Sağ üstteki tutar; tamamlanmış yolculukta "₺110,50", aksi halde durum etiketi ("Aktif" vb.). */
@Composable
private fun TrailingAmount(rental: RentalHistoryItemUi) {
    if (rental.priceLabel != null) {
        Text(
            text = rental.priceLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else if (rental.statusLabel != null) {
        Text(
            text = rental.statusLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Süre/mesafe rozeti (mat gri kapsül). */
@Composable
private fun MetricPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Kartın solundaki DEKORATİF rota küçük görseli: mat harita zemini + stilize mavi güzergâh + uç
 * noktaları. API'de kiralamaya ait gerçek rota/polyline yoktur (AGENTS §2.2), bu yüzden çizim
 * temsilîdir — gerçek yol olduğu iddia edilmez (bkz. decisions.md dekoratif emsalleri).
 */
@Composable
private fun RouteThumbnail(modifier: Modifier = Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val routeColor = RencarBlue
    val startColor = MaterialTheme.rencar.success
    val endRing = MaterialTheme.colorScheme.surfaceContainerLow
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val unit = size.minDimension

            // Silik sokak ızgarası (dekoratif harita hissi).
            val gridStroke = unit * 0.02f
            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = gridStroke)
            drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth = gridStroke)

            // Güzergâh: sol-alt → sağ-üst yumuşak S kıvrımı.
            val start = Offset(w * 0.20f, h * 0.78f)
            val end = Offset(w * 0.80f, h * 0.24f)
            val route = Path().apply {
                moveTo(start.x, start.y)
                cubicTo(w * 0.34f, h * 0.30f, w * 0.58f, h * 0.72f, end.x, end.y)
            }
            drawPath(route, color = routeColor, style = Stroke(width = unit * 0.06f, cap = StrokeCap.Round))

            // Uç noktaları: başlangıç yeşil, bitiş mavi (beyaz halkalı).
            drawCircle(startColor, radius = unit * 0.075f, center = start)
            drawCircle(endRing, radius = unit * 0.09f, center = end)
            drawCircle(routeColor, radius = unit * 0.06f, center = end)
        }
    }
}

// ── Boş durum: henüz kiralama yok ──
@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RencarBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.History,
                contentDescription = null,
                tint = RencarBlue,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Henüz kiralaman yok.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tamamladığın yolculuklar burada listelenir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewRentals = listOf(
    RentalHistoryItemUi("1", "Renault Clio", "26 Haz 2026 · 14:32", "24 dk", "12,4 km", "₺110,50", null),
    RentalHistoryItemUi("2", "Fiat Egea", "24 Haz 2026 · 18:05", "18 dk", "8,1 km", "₺86,00", null),
    RentalHistoryItemUi("3", "Volkswagen Polo", "21 Haz 2026 · 09:48", "31 dk", "19,6 km", "₺142,00", null),
    RentalHistoryItemUi("4", "Renault Clio", "18 Haz 2026 · 20:14", "3 dk", "0,4 km", null, "Aktif"),
)

private val PreviewStats = RentalStatsUi(tripCount = 6, summaryLabel = "Bu ay 6 yolculuk · ₺612 harcama")

@Preview(name = "Kiralamalarım · Light", showBackground = true, heightDp = 820)
@Composable
private fun RentalsLightPreview() {
    RenCarTheme(darkTheme = false) {
        RentalsScreen(
            uiState = RentalsUiState(isLoading = false, rentals = PreviewRentals, stats = PreviewStats),
            onIntent = {},
        )
    }
}

@Preview(name = "Kiralamalarım · Dark", showBackground = true, heightDp = 820)
@Composable
private fun RentalsDarkPreview() {
    RenCarTheme(darkTheme = true) {
        RentalsScreen(
            uiState = RentalsUiState(isLoading = false, rentals = PreviewRentals, stats = PreviewStats),
            onIntent = {},
        )
    }
}

@Preview(name = "Kiralamalarım · Boş", showBackground = true, heightDp = 500)
@Composable
private fun RentalsEmptyPreview() {
    RenCarTheme(darkTheme = false) {
        RentalsScreen(
            uiState = RentalsUiState(
                isLoading = false,
                rentals = emptyList(),
                stats = RentalStatsUi(tripCount = 0, summaryLabel = "Bu ay 0 yolculuk · ₺0 harcama"),
            ),
            onIntent = {},
        )
    }
}
