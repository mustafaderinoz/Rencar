package com.turkcell.rencar.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightOnPrimary
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/** Kullanıcıya hızlı başlangıç için sunulan örnek sorgu çipleri. */
private val QUICK_SUGGESTIONS = listOf(
    "2000 TL altı konforlu araç",
    "Araziye çıkacağım, SUV lazım",
    "Uygun fiyatlı, otomatik vites",
    "Kalabalık aile için geniş araç",
)

/**
 * AI destekli araç önerisi diyaloğu.
 *
 * Not (bug fix): [viewModel] Activity/NavBackStackEntry scope'unda hayatta kaldığından,
 * dialog kapansa bile önceki sorgunun sonucu ([AiRecommendationUiState.recommendedIds])
 * bellekte kalır. Bu yüzden dialog her açıldığında (composable ilk composition'a
 * girdiğinde) state mutlaka [AiRecommendationIntent.Clear] ile sıfırlanır — aksi halde
 * eski dolu recommendedIds, aşağıdaki "sonucu uygula" effect'ini mount olur olmaz
 * tetikleyip diyaloğu anında kapatıyordu.
 */
@Composable
fun AiRecommendationDialog(
    vehicles: List<VehicleUi>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit,
    viewModel: AiRecommendationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Diyalog her kapandığında (herhangi bir sebeple) state'i temizle.
    // Bu sayede bir sonraki açılışta eski sonuçlar/sorgu görülmez.
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onIntent(AiRecommendationIntent.Clear)
        }
    }

    // Harita ekranından gelen aday araçları VM state'ine ver; Submit bu listeden seçer (araç
    // listesi artık intent payload'ı değil, state üzerinden akar — tek doğruluk kaynağı).
    LaunchedEffect(vehicles) {
        viewModel.onIntent(AiRecommendationIntent.VehiclesProvided(vehicles))
    }

    // Yeni bir öneri sonucu geldiğinde haritaya uygula ve diyaloğu kapat.
    LaunchedEffect(uiState.recommendedIds) {
        if (uiState.recommendedIds.isNotEmpty()) {
            onApply(uiState.recommendedIds.toSet())
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AiRecommendationDialogContent(
            uiState = uiState,
            onIntent = { intent ->
                when (intent) {
                    AiRecommendationIntent.Dismiss -> onDismiss()
                    else -> viewModel.onIntent(intent)
                }
            },
        )
    }
}

/**
 * Diyaloğun görsel içeriği. State-hoisted: önizlemede kullanılabilmesi için
 * ViewModel/Dialog'dan ayrıştırıldı.
 */
@Composable
private fun AiRecommendationDialogContent(
    uiState: AiRecommendationUiState,
    onIntent: (AiRecommendationIntent) -> Unit,
) {
    // §4.5: gövde yalnız (uiState, onIntent) alır; okunabilirlik için alanlar/aksiyonlar yerelde açılır.
    val query = uiState.query
    val isLoading = uiState.isLoading
    val error = uiState.error
    val onQueryChanged: (String) -> Unit = { onIntent(AiRecommendationIntent.QueryChanged(it)) }
    val onSubmit: () -> Unit = { onIntent(AiRecommendationIntent.Submit) }
    val onDismiss: () -> Unit = { onIntent(AiRecommendationIntent.Dismiss) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clip(RoundedCornerShape(24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Üst satır: gradyanlı AI rozeti + kapat butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(LightPrimary, LightPrimary.copy(alpha = 0.6f))
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RencarIcons.Sparkles,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(RencarIcons.Close, contentDescription = "Kapat")
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Sana en uygun aracı bulalım",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "İhtiyacını kendi cümlelerinle anlat, senin için filtreleyelim.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Örn: 2000 TL altı konforlu araç arıyorum") },
                enabled = !isLoading,
                minLines = 2,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LightPrimary,
                    cursorColor = LightPrimary,
                ),
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Hızlı öneri çipleri
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QUICK_SUGGESTIONS) { label ->
                    SuggestionChip(
                        onClick = { if (!isLoading) onQueryChanged(label) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        enabled = !isLoading,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Alt satır: Vazgeç / Ara butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Vazgeç")
                }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1.5f),
                    enabled = query.isNotBlank() && !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LightPrimary,
                        contentColor = LightOnPrimary,
                    ),
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = RencarIcons.Sparkles,
                            contentDescription = null,
                            tint = LightOnPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Ara")
                    }
                }
            }
        }
    }
}

@Preview(name = "AiRecommendationDialog · Boş", showBackground = true)
@Composable
private fun AiRecommendationDialogEmptyPreview() {
    RenCarTheme {
        AiRecommendationDialogContent(
            uiState = AiRecommendationUiState(),
            onIntent = {},
        )
    }
}

@Preview(name = "AiRecommendationDialog · Yükleniyor", showBackground = true)
@Composable
private fun AiRecommendationDialogLoadingPreview() {
    RenCarTheme {
        AiRecommendationDialogContent(
            uiState = AiRecommendationUiState(query = "2000 TL altı konforlu araç arıyorum", isLoading = true),
            onIntent = {},
        )
    }
}

@Preview(name = "AiRecommendationDialog · Hata", showBackground = true)
@Composable
private fun AiRecommendationDialogErrorPreview() {
    RenCarTheme {
        AiRecommendationDialogContent(
            uiState = AiRecommendationUiState(query = "araziye çıkacağım", error = "Öneri alınamadı: zaman aşımı"),
            onIntent = {},
        )
    }
}