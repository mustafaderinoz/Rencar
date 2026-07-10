package com.turkcell.rencar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary

/** Marka mavisi — tema-bağımsız (bkz. Login/OTP). */
private val RencarBlue = LightPrimary

/** Ehliyet doğrulama akışının 3 adımı. */
enum class VerificationStep(val label: String) {
    LICENSE("Ehliyet"),
    SELFIE("Selfie"),
    APPROVAL("Onay"),
}

/**
 * Ehliyet doğrulama akışının üst adım göstergesi (1 Ehliyet · 2 Selfie · 3 Onay).
 * Hem License hem Selfie ekranı kullanır. Tamamlanan adım mavi tik, aktif adım mavi
 * numara, gelecek adım gri gösterilir.
 *
 * @param currentStep o an aktif olan adım.
 */
@Composable
fun VerificationStepper(
    currentStep: VerificationStep,
    modifier: Modifier = Modifier,
) {
    val steps = VerificationStep.entries
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { index, step ->
            val state = when {
                index < currentStep.ordinal -> StepState.COMPLETED
                index == currentStep.ordinal -> StepState.ACTIVE
                else -> StepState.UPCOMING
            }
            StepNode(number = index + 1, label = step.label, state = state)

            // Adımlar arası bağlayıcı çizgi (son adımdan sonra yok).
            if (index < steps.lastIndex) {
                val connectorActive = index < currentStep.ordinal
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(
                            color = if (connectorActive) {
                                RencarBlue
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

private enum class StepState { COMPLETED, ACTIVE, UPCOMING }

@Composable
private fun StepNode(number: Int, label: String, state: StepState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = when (state) {
                        StepState.UPCOMING -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> RencarBlue
                    },
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                StepState.COMPLETED -> Icon(
                    imageVector = RencarIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )

                else -> Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (state == StepState.ACTIVE) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (state == StepState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
            color = when (state) {
                StepState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
