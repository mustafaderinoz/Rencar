package com.turkcell.rencar.ui.licensepending

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Ehliyeti incelemede (UNDER_REVIEW) olan PENDING kullanıcı için engelleyici bekleme ekranı.
 * Kullanıcı ehliyeti onaylanana kadar uygulamayı kullanamaz: [BackHandler] geri tuşunu yutar ve
 * bu rotaya girilirken geri yığını temizlendiği için başka bir ekrana dönülemez. Saf bilgi
 * ekranıdır; kullanıcı aksiyonu yoktur (§4.6: state/intent/VM eklenmez).
 *
 * Marka mavisi (Rencar Blue) her iki temada aynı olduğundan tema-bağımsız [LightPrimary]
 * token'ı kullanılır (`colorScheme.primary` koyu temada pastele döner).
 */
@Composable
fun LicensePendingScreen() {
    // Geri tuşunu tamamen etkisizleştir: bu ekrandan çıkılamaz.
    BackHandler(enabled = true) { /* yut: hiçbir şey yapma */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = LightPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Clock,
                    contentDescription = null,
                    tint = LightPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Ehliyetiniz doğrulama için bekliyor",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Ehliyet başvurunuz inceleniyor. Onaylandığında uygulamayı " +
                    "kullanmaya başlayabilirsiniz.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(name = "License Pending · Light", showBackground = true)
@Composable
private fun LicensePendingLightPreview() {
    RenCarTheme(darkTheme = false) { LicensePendingScreen() }
}

@Preview(name = "License Pending · Dark", showBackground = true)
@Composable
private fun LicensePendingDarkPreview() {
    RenCarTheme(darkTheme = true) { LicensePendingScreen() }
}
