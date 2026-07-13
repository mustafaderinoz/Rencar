package com.turkcell.rencar.ui.profile

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar

/**
 * Marka mavisi — avatar aksanı her iki temada AYNI mavidir (colorScheme.primary koyu temada
 * pastele döner). Tema-bağımsız marka token'ı [LightPrimary] kullanılır (bkz. Login/Onboarding).
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5): açılışta profili yükler, stateless gövdeyi besler ──
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sekme ilk açıldığında profil + ehliyet durumu çekilir.
    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileIntent.Load)
    }

    ProfileScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun ProfileScreen(
    uiState: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        when {
            uiState.isLoading && uiState.fullName.isEmpty() -> LoadingState()
            uiState.errorMessage != null && uiState.fullName.isEmpty() ->
                ErrorState(message = uiState.errorMessage, onRetry = { onIntent(ProfileIntent.Retry) })

            else -> ProfileContent(uiState = uiState)
        }
    }
}

// ── İçerik: başlık + ehliyet kartı + menü + çıkış ──
@Composable
private fun ProfileContent(uiState: ProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        ProfileHeader(fullName = uiState.fullName, phone = uiState.phone)

        Spacer(Modifier.height(20.dp))

        LicenseCard(status = uiState.licenseStatus)

        Spacer(Modifier.height(16.dp))

        // ── Menü (statik) ──
        Card {
            MenuRow(icon = RencarIcons.CreditCard, label = "Ödeme yöntemleri")
            MenuDivider()
            MenuRow(icon = RencarIcons.Gear, label = "Ayarlar")
            MenuDivider()
            MenuRow(icon = RencarIcons.Help, label = "Yardım & destek")
            MenuDivider()
            MenuRow(icon = RencarIcons.Gift, label = "Davet et · ₺50 kazan")
        }

        Spacer(Modifier.height(16.dp))

        // ── Çıkış yap (statik) ──
        Card {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = RencarIcons.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Çıkış yap",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ── Başlık: avatar (baş harfler) + ad/telefon + düzenle butonu ──
@Composable
private fun ProfileHeader(fullName: String, phone: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(RencarBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialsOf(fullName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RencarBlue,
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (phone.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Düzenle (statik) — Login geri butonuyla aynı yumuşak kare kalıbı.
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp),
                )
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Edit,
                contentDescription = "Profili düzenle",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Ehliyet doğrulama kartı (dinamik): ikon + başlık + durum rozeti ──
@Composable
private fun LicenseCard(status: LicenseVerificationStatus) {
    val visuals = licenseVisuals(status)
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(visuals.containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Shield,
                    contentDescription = null,
                    tint = visuals.iconColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Text(
                text = visuals.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (visuals.badgeLabel != null) {
                Spacer(Modifier.width(12.dp))
                StatusPill(
                    label = visuals.badgeLabel,
                    background = visuals.badgeBackground,
                    foreground = visuals.badgeForeground,
                )
            }
        }
    }
}

// ── Durum rozeti (pill) ──
@Composable
private fun StatusPill(label: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = foreground,
        )
    }
}

// ── Menü satırı: ikon + etiket + ileri oku (statik, tıklanabilir no-op) ──
@Composable
private fun MenuRow(icon: ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = RencarIcons.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

// ── Beyaz yuvarlak-köşe kart kabuğu (tema uyumlu: containerLowest zemin) ──
@Composable
private fun Card(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        content = content,
    )
}

// ── Yükleniyor / Hata durumları ──
@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = RencarBlue, strokeWidth = 2.dp)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
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

/** Ad-soyaddan avatar baş harfleri: "Deniz Yılmaz" → "DY" (ilk iki kelime). */
private fun initialsOf(fullName: String): String =
    fullName.trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }

/** Ehliyet durumunun görsel demeti (başlık + rozet + renkler). */
private data class LicenseVisuals(
    val title: String,
    val badgeLabel: String?,
    val iconColor: Color,
    val containerColor: Color,
    val badgeBackground: Color,
    val badgeForeground: Color,
)

@Composable
private fun licenseVisuals(status: LicenseVerificationStatus): LicenseVisuals = when (status) {
    LicenseVerificationStatus.APPROVED -> LicenseVisuals(
        title = "Ehliyet doğrulandı",
        badgeLabel = "Onaylı",
        iconColor = MaterialTheme.rencar.success,
        containerColor = MaterialTheme.rencar.successContainer,
        badgeBackground = MaterialTheme.rencar.successContainer,
        badgeForeground = MaterialTheme.rencar.onSuccessContainer,
    )

    LicenseVerificationStatus.UNDER_REVIEW -> LicenseVisuals(
        title = "Ehliyet inceleniyor",
        badgeLabel = "İncelemede",
        iconColor = MaterialTheme.rencar.warning,
        containerColor = MaterialTheme.rencar.warningContainer,
        badgeBackground = MaterialTheme.rencar.warningContainer,
        badgeForeground = MaterialTheme.rencar.onWarningContainer,
    )

    LicenseVerificationStatus.REJECTED -> LicenseVisuals(
        title = "Ehliyet reddedildi",
        badgeLabel = "Reddedildi",
        iconColor = MaterialTheme.colorScheme.error,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        badgeBackground = MaterialTheme.colorScheme.errorContainer,
        badgeForeground = MaterialTheme.colorScheme.onErrorContainer,
    )

    LicenseVerificationStatus.NOT_SUBMITTED -> LicenseVisuals(
        title = "Ehliyet doğrulanmadı",
        badgeLabel = "Bekliyor",
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        badgeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
        badgeForeground = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    LicenseVerificationStatus.UNKNOWN -> LicenseVisuals(
        title = "Ehliyet durumu",
        badgeLabel = null,
        iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        badgeBackground = MaterialTheme.colorScheme.surfaceContainerHighest,
        badgeForeground = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewState = ProfileUiState(
    fullName = "Deniz Yılmaz",
    phone = "+90 532 000 00 00",
    licenseStatus = LicenseVerificationStatus.APPROVED,
)

@Preview(name = "Profile · Light", showBackground = true)
@Composable
private fun ProfileScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        ProfileScreen(uiState = PreviewState, onIntent = {})
    }
}

@Preview(name = "Profile · Dark", showBackground = true)
@Composable
private fun ProfileScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        ProfileScreen(
            uiState = PreviewState.copy(licenseStatus = LicenseVerificationStatus.UNDER_REVIEW),
            onIntent = {},
        )
    }
}
