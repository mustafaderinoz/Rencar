package com.turkcell.rencar.ui.activerental

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.RentalReceiptUi
import com.turkcell.rencar.data.model.VehiclePoint
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.map.RencarMap
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.util.Locale
import org.maplibre.android.geometry.LatLng

/** Marka mavisi — tema-bağımsız (bkz. Login/OTP/License/RentalPhotos). */
private val RencarBlue = LightPrimary

/** "Kiralamayı Bitir" mercan kırmızısı — tasarımdaki tehlike/aksiyon rengi (tema-bağımsız). */
private val DangerRed = Color(0xFFF1584F)

// ── Stateful sarmalayıcı (§4.5): state'i toplar, intent'leri VM'e iletir ──
@Composable
fun ActiveRentalScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActiveRentalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ActiveRentalScreen(
        uiState = uiState,
        onBack = onNavigateBack,
        onIntent = viewModel::onIntent,
    )
}

// ── Stateless gövde (§4.5): uiState + onIntent ile çizer ──
@Composable
private fun ActiveRentalScreen(
    uiState: ActiveRentalUiState,
    onBack: () -> Unit,
    onIntent: (ActiveRentalIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        TopBar(onBack = onBack)

        val firstLoad = uiState.vehicleTitle.isEmpty()
        when {
            firstLoad && uiState.loadError != null ->
                ErrorState(
                    message = uiState.loadError,
                    onRetry = { onIntent(ActiveRentalIntent.Retry) },
                    modifier = Modifier.weight(1f),
                )

            firstLoad && uiState.isLoading ->
                LoadingState(Modifier.weight(1f))

            else -> Content(
                uiState = uiState,
                onIntent = onIntent,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Üst başlık: ‹ geri + "Aktif Yolculuk" / "Süre ve ücret canlı işliyor" ──
@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Aktif Yolculuk",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Süre ve ücret canlı işliyor",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── İçerik: araç kartı, harita, geçen süre, ücret/mesafe, bilgi, alt aksiyonlar ──
@Composable
private fun Content(
    uiState: ActiveRentalUiState,
    onIntent: (ActiveRentalIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))
        VehicleCard(uiState)
        Spacer(Modifier.height(14.dp))
        MapCard(uiState.vehiclePoint)
        Spacer(Modifier.height(14.dp))
        ElapsedCard(uiState)
        Spacer(Modifier.height(16.dp))
        MetricsRow(uiState)
        Spacer(Modifier.height(14.dp))

        when {
            uiState.isFinished -> FinishedBanner(uiState.receipt)
            // Simülasyon henüz başlamadı → kullanıcıyı "Kilitle / Aç" ile başlatmaya yönlendir.
            !uiState.started -> StartHintBanner()
            else -> StartFeeBanner(uiState.startFee)
        }

        Spacer(Modifier.weight(1f))

        uiState.finishError?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        BottomActions(uiState = uiState, onIntent = onIntent)
        Spacer(Modifier.height(12.dp))
    }
}

// ── Araç kartı: araç ikonu + "Renault Clio" / "34 HCH 305 · Dakikalık" ──
@Composable
private fun VehicleCard(uiState: ActiveRentalUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Car,
                contentDescription = null,
                tint = RencarBlue,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = uiState.vehicleTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = vehicleSubtitle(uiState),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Harita kartı: aktif yolculuk aracının canlı konumu (Socket.IO) ──
@Composable
private fun MapCard(point: VehiclePoint?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        RencarMap(
            myLocation = null,
            modifier = Modifier.fillMaxSize(),
            ridePoint = point?.let { LatLng(it.latitude, it.longitude) },
        )
    }
}

// ── Geçen süre kartı: "Geçen süre" / 00:01:12 / "Başlangıç: 14.07.2026 15:55" ──
@Composable
private fun ElapsedCard(uiState: ActiveRentalUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Geçen süre",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatElapsed(uiState.elapsedSeconds),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        uiState.startedAtLabel?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Başlangıç: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── Ücret + mesafe satırı ──
@Composable
private fun MetricsRow(uiState: ActiveRentalUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 14.dp),
    ) {
        Metric(
            label = if (uiState.isFinished) "Toplam ücret" else "Anlık ücret",
            value = formatCost(uiState.currentCost),
            valueColor = RencarBlue,
            modifier = Modifier.weight(1f),
        )
        Metric(
            label = "Mesafe",
            value = formatDistance(uiState.distanceKm),
            valueColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
    }
}

// ── Başlamadan önce: kullanıcıyı "Kilitle / Aç" ile yolculuğu başlatmaya yönlendiren ipucu (mavi) ──
@Composable
private fun StartHintBanner() {
    InfoBanner(
        icon = RencarIcons.Lock,
        iconTint = RencarBlue,
        background = RencarBlue.copy(alpha = 0.08f),
        text = "Yolculuğu başlatmak için aşağıdaki “Kilitle / Aç” düğmesine dokunun; " +
            "süre ve ücret bundan sonra işlemeye başlar.",
    )
}

// ── Bilgi kartı: başlangıç ücreti açıklaması (mavi) ──
@Composable
private fun StartFeeBanner(startFee: Double) {
    InfoBanner(
        icon = RencarIcons.Info,
        iconTint = RencarBlue,
        background = RencarBlue.copy(alpha = 0.08f),
        text = "Anlık ücrete ${formatFee(startFee)} ₺ başlangıç ücreti dahildir; " +
            "kesin döküm bitince çıkar.",
    )
}

// ── Yolculuk bitiş özeti (yeşil) ──
@Composable
private fun FinishedBanner(receipt: RentalReceiptUi?) {
    val total = receipt?.totalPrice ?: 0.0
    InfoBanner(
        icon = RencarIcons.Check,
        iconTint = MaterialTheme.rencar.success,
        background = MaterialTheme.rencar.successContainer.copy(alpha = 0.5f),
        text = "Yolculuk tamamlandı — toplam ${formatCost(total)}. Ödeme adımı yakında.",
    )
}

@Composable
private fun InfoBanner(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    background: Color,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Alt aksiyonlar: "Kilitle / Aç" (yerel) + "Kiralamayı Bitir" ──
@Composable
private fun BottomActions(
    uiState: ActiveRentalUiState,
    onIntent: (ActiveRentalIntent) -> Unit,
) {
    if (uiState.isFinished) {
        // Yolculuk bitti: ödeme ekranı henüz yok → devre dışı bilgilendirme butonu (ekranda kalınır).
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(
                text = "Ödeme adımı yakında",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // "Kilitle / Aç" — API ucu yok; yalnız yerel görsel durum. Kilitliyken marka mavisi,
        // açıkken sönük gri (dokununca durum görünür değişir).
        val lockTint = if (uiState.locked) RencarBlue else MaterialTheme.colorScheme.onSurfaceVariant
        OutlinedButton(
            onClick = { onIntent(ActiveRentalIntent.LockToggle) },
            enabled = !uiState.isFinishing,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = lockTint),
        ) {
            Icon(
                imageVector = RencarIcons.Lock,
                contentDescription = null,
                tint = lockTint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kilitle / Aç",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Button(
            onClick = { onIntent(ActiveRentalIntent.FinishClicked) },
            // Simülasyon başlamadan (started=false) bitirilecek bir şey yok → pasif.
            enabled = uiState.started && !uiState.isFinishing,
            modifier = Modifier
                .weight(1.3f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = DangerRed,
                contentColor = Color.White,
                disabledContainerColor = DangerRed.copy(alpha = 0.5f),
                disabledContentColor = Color.White,
            ),
        ) {
            if (uiState.isFinishing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
            } else {
                Text(
                    text = "Kiralamayı Bitir",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
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

/** "34 HCH 305 · Dakikalık"; alanlar boşsa nokta ayırıcı atlanır. */
private fun vehicleSubtitle(uiState: ActiveRentalUiState): String =
    listOf(uiState.vehiclePlate, uiState.planLabel)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

/** Saniyeyi "HH:MM:SS" biçimine çevirir. */
private fun formatElapsed(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
}

/** Ücret: nokta ondalık, "24.50 ₺" (tasarımla birebir). */
private fun formatCost(value: Double): String = "%.2f ₺".format(Locale.US, value)

/** Mesafe: virgül ondalık, "6,5 km" (tasarımla birebir). */
private fun formatDistance(km: Double): String = "%.1f km".format(Locale.forLanguageTag("tr"), km)

/** Başlangıç ücreti tamsayı gösterimi (ör. "15"). */
private fun formatFee(value: Double): String = "%.0f".format(value)

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5). RencarMap önizlemede placeholder. ──
private val PreviewState = ActiveRentalUiState(
    started = true,
    isLoading = false,
    rentalId = "clx0rent1234567890",
    vehicleTitle = "Renault Clio",
    vehiclePlate = "34 HCH 305",
    planLabel = "Dakikalık",
    startedAtLabel = "14.07.2026 15:55",
    startFee = 15.0,
    distanceKm = 6.5,
    currentCost = 24.5,
    elapsedSeconds = 72,
)

@Preview(name = "ActiveRental · Light", showBackground = true, heightDp = 820)
@Composable
private fun ActiveRentalLightPreview() {
    RenCarTheme(darkTheme = false) {
        ActiveRentalScreen(uiState = PreviewState, onBack = {}, onIntent = {})
    }
}

@Preview(name = "ActiveRental · Dark", showBackground = true, heightDp = 820)
@Composable
private fun ActiveRentalDarkPreview() {
    RenCarTheme(darkTheme = true) {
        ActiveRentalScreen(uiState = PreviewState, onBack = {}, onIntent = {})
    }
}
