package com.turkcell.rencar.ui.onboarding

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette; navigation.compose'daki
// kopya deprecated. Bağımlılık hilt-navigation-compose (decisions.md) buna transitively bağımlı.
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Marka mavisi (Rencar Blue, 0xFF1A6BF0). Tasarımda logo/buton/aktif nokta/bağlantı
 * her iki temada da AYNI mavidir; `colorScheme.primary` koyu temada pastele döndüğü
 * için tema-bağımsız marka token'ı (LightPrimary) kullanılır.
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingScreen(
        uiState = uiState,
        onIntent = viewModel::onIntent,
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun OnboardingScreen(
    uiState: OnboardingUiState,
    onIntent: (OnboardingIntent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // ── Kahraman bölge: logo + başlık + alt metin ──
            RencarLogo()

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Rencar",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Yakındaki aracı bul,\ndakikalar içinde yola çık.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            // ── Alt bölge: sayfa göstergesi + aksiyonlar ──
            PageIndicator(
                currentPage = uiState.currentPage,
                pageCount = uiState.pageCount,
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { onIntent(OnboardingIntent.StartClicked) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RencarBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Hemen Başla",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Zaten hesabım var · ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Giriş yap",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RencarBlue,
                    modifier = Modifier.clickable { onIntent(OnboardingIntent.LoginClicked) },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Marka logosu: mavi kare + arkasında yumuşak radyal ışıma + beyaz araç ──
@Composable
private fun RencarLogo() {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(RencarBlue.copy(alpha = 0.30f), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(RencarBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = RencarIcons.Car,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp),
            )
        }
    }
}

// ── 3 noktalı sayfa göstergesi (aktif = mavi pill) ──
@Composable
private fun PageIndicator(
    currentPage: Int,
    pageCount: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val active = index == currentPage
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) RencarBlue
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
                    ),
            )
        }
    }
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
@Preview(name = "Onboarding · Light", showBackground = true)
@Composable
private fun OnboardingScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        OnboardingScreen(uiState = OnboardingUiState(), onIntent = {})
    }
}

@Preview(name = "Onboarding · Dark", showBackground = true)
@Composable
private fun OnboardingScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        OnboardingScreen(uiState = OnboardingUiState(), onIntent = {})
    }
}
