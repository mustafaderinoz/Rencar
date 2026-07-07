package com.turkcell.rencar.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Aktif sekme rengi — tasarımda marka mavisi (Rencar Blue, 0xFF1A6BF0). `colorScheme.primary`
 * koyu temada pastele döndüğü için tema-bağımsız marka token'ı (LightPrimary) kullanılır
 * (bkz. Onboarding/OTP ekranları).
 */
private val RencarBlue = LightPrimary

/**
 * Alt navigasyon sekmeleri: route + etiket + ikon. Ekran görüntüsündeki dört sekme
 * (Harita · Geçmiş · Cüzdan · Profil), soldan sağa aynı sırada.
 */
enum class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Map(RencarDestinations.MAP, "Harita", RencarIcons.MapPin),
    History(RencarDestinations.HISTORY, "Geçmiş", RencarIcons.History),
    Wallet(RencarDestinations.WALLET, "Cüzdan", RencarIcons.Wallet),
    Profile(RencarDestinations.PROFILE, "Profil", RencarIcons.Person),
}

/**
 * Tasarıma sadık özel alt navigasyon çubuğu: koyu zemin, sekme başına ikon + etiket,
 * aktif sekme mavi / pasif sekmeler ikincil gri. M3 [androidx.compose.material3.NavigationBar]
 * kapsül (pill) highlight çizdiği için tasarımdaki düz görünüme uymaz; bu yüzden Row tabanlı.
 */
@Composable
fun RencarBottomBar(
    currentRoute: String?,
    onTabSelected: (BottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
        ) {
            BottomTab.entries.forEach { tab ->
                BottomBarItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

// ── Tek sekme: ikon + etiket dikey dizili, ripple'sız tıklama ──
@Composable
private fun RowScope.BottomBarItem(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = if (selected) RencarBlue else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = color,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Preview(name = "BottomBar · Dark", showBackground = true)
@Composable
private fun RencarBottomBarDarkPreview() {
    RenCarTheme(darkTheme = true) {
        RencarBottomBar(currentRoute = RencarDestinations.PROFILE, onTabSelected = {})
    }
}

@Preview(name = "BottomBar · Light", showBackground = true)
@Composable
private fun RencarBottomBarLightPreview() {
    RenCarTheme(darkTheme = false) {
        RencarBottomBar(currentRoute = RencarDestinations.MAP, onTabSelected = {})
    }
}
