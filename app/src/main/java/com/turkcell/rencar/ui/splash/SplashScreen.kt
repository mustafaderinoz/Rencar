package com.turkcell.rencar.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * 00 Açılış (session restore) ekranı — §4.5 stateful + stateless çifti.
 *
 * Açılışta oturum geri-yüklenirken marka logosu + yükleniyor göstergesi çizer. Karar verilince
 * ([SplashUiState.destination]) navigasyonu callback ile üst katmana (NavHost) bildirir; ağ
 * hatasında "Tekrar Dene" gösterir. NavController bu composable'a sızmaz (tek sorumluluk).
 *
 * Marka mavisi her iki temada aynı olduğundan tema-bağımsız [LightPrimary] kullanılır
 * (`colorScheme.primary` koyu temada pastele döner).
 */
@Composable
fun SplashScreen(
    onDestinationResolved: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.destination) {
        uiState.destination?.let { destination ->
            onDestinationResolved(destination)
            viewModel.onIntent(SplashIntent.DestinationHandled)
        }
    }

    SplashContent(uiState = uiState, onIntent = viewModel::onIntent)
}

@Composable
private fun SplashContent(
    uiState: SplashUiState,
    onIntent: (SplashIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "RenCar",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = LightPrimary,
        )

        Spacer(Modifier.height(32.dp))

        if (uiState.isError) {
            Text(
                text = "Oturumunuz doğrulanamadı",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "İnternet bağlantınızı kontrol edip tekrar deneyin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { onIntent(SplashIntent.Retry) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LightPrimary,
                    contentColor = Color.White,
                ),
            ) {
                Text("Tekrar Dene")
            }
        } else {
            CircularProgressIndicator(color = LightPrimary)
        }
    }
}

@Preview(name = "Splash · Loading · Light", showBackground = true)
@Composable
private fun SplashLoadingLightPreview() {
    RenCarTheme(darkTheme = false) {
        SplashContent(uiState = SplashUiState(isLoading = true), onIntent = {})
    }
}

@Preview(name = "Splash · Error · Dark", showBackground = true)
@Composable
private fun SplashErrorDarkPreview() {
    RenCarTheme(darkTheme = true) {
        SplashContent(uiState = SplashUiState(isLoading = false, isError = true), onIntent = {})
    }
}
