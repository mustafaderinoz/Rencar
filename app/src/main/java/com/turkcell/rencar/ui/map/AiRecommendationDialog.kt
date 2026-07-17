package com.turkcell.rencar.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.data.model.VehicleUi
import com.turkcell.rencar.ui.theme.LightOnPrimary
import com.turkcell.rencar.ui.theme.LightPrimary

/**
 * AI Önerisi Diyaloğu: Kullanıcıdan metin girişi alır ve sonuçları MapScreen'e iletir.
 */
@Composable
fun AiRecommendationDialog(
    vehicles: List<VehicleUi>,
    onDismiss: () -> Unit,
    onResult: (Set<String>) -> Unit,
    viewModel: AiRecommendationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Diyalog ilk açıldığında veya kapatıldığında state'i sıfırla ki 
    // eski sonuçlar yüzünden hemen kapanmasın.
    LaunchedEffect(Unit) {
        viewModel.onIntent(AiRecommendationIntent.Clear, vehicles)
    }

    // Öneriler geldiyse üst katmana bildir ve kapat
    LaunchedEffect(uiState.recommendedIds) {
        if (uiState.recommendedIds.isNotEmpty()) {
            onResult(uiState.recommendedIds.toSet())
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Araç Önerisi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                Text(
                    text = "Nasıl bir araç arıyorsun? Örn: '4 kişilik 1500 TL altı araçlar'",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onIntent(AiRecommendationIntent.QueryChanged(it), vehicles) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buraya yazın...") },
                    enabled = !uiState.isLoading,
                    shape = RoundedCornerShape(12.dp)
                )

                if (uiState.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vazgeç")
                    }
                    
                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = { viewModel.onIntent(AiRecommendationIntent.Submit, vehicles) },
                        modifier = Modifier.weight(1.5f),
                        enabled = uiState.query.isNotBlank() && !uiState.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightPrimary,
                            contentColor = LightOnPrimary
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Ara")
                        }
                    }
                }
            }
        }
    }
}
