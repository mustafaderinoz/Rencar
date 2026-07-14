package com.turkcell.rencar.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.CatComfort
import com.turkcell.rencar.ui.theme.CatEconomy
import com.turkcell.rencar.ui.theme.CatSuv
import com.turkcell.rencar.ui.theme.LightOnPrimary
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Ortalama yürüme hızı (m/dk) — en yakın araca "~N dk" tahmini için. */
private const val WALK_METERS_PER_MIN = 80f

/**
 * Segment çipi: etiket → API `segment` değeri + tasarımdaki renkli nokta. "Tümü" filtresizdir
 * (value null, nokta yok). Renkler tema bağımsız kategori tokenlarıdır (Color.kt).
 */
private data class SegmentChipInfo(val label: String, val value: String?, val dot: Color?)

private val SEGMENT_CHIPS = listOf(
    SegmentChipInfo("Tümü", null, null),
    SegmentChipInfo("Ekonomik", "ECONOMY", CatEconomy),
    SegmentChipInfo("Konfor", "COMFORT", CatComfort),
    SegmentChipInfo("SUV", "SUV", CatSuv),
)

/**
 * Harita ekranının altındaki kalıcı kart (tasarımdaki alt panel). "Yakınında N araç" başlığı,
 * mahalle + ~dk altyazısı, segment filtre çipleri ve "En Yakın Aracı Bul" butonunu içerir.
 * Saf stateless bileşen: veriyi [availableCount]/[localityName]/[nearestDistanceMeters] ve
 * seçili segmenti alır, aksiyonları callback ile yukarı iletir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapBottomCard(
    availableCount: Int,
    localityName: String?,
    nearestDistanceMeters: Float?,
    selectedSegment: String?,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSegmentSelected: (String?) -> Unit,
    onFindNearest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Tutamaç — HER ZAMAN tek ve kararlı öğe olarak burada durur; açıkken kartın hemen
        // üstünde (panele bitişik), kapalıyken haritada yüzer. Tek öğe olduğundan aç/kapa
        // sırasında zıplama/havada asılma olmaz. Dokununca barı aç/kapa yapar.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = if (expanded) 0.dp else 6.dp,
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        ) {
            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp)) {
                HandleBar()
            }
        }

        // Bar paneli — yüksekliği açılıp kapanır (slide değil); kararlı tutamacın altında
        // akıcı biçimde iner/kalkar. Kapalıyken hiç çizilmez.
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                tonalElevation = 3.dp,
                shadowElevation = 12.dp,
            ) {
                Column(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
                ) {

                    // Başlık + altyazı.
                    Text(
                        text = "Yakınında $availableCount araç",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val subtitle = buildSubtitle(localityName, nearestDistanceMeters)
                    if (subtitle != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Segment filtre çipleri (renkli noktalı) — yatay kaydırılabilir.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SEGMENT_CHIPS.forEach { chip ->
                            FilterChip(
                                selected = selectedSegment == chip.value,
                                onClick = { onSegmentSelected(chip.value) },
                                label = { Text(chip.label) },
                                leadingIcon = chip.dot?.let { dot ->
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(dot, CircleShape),
                                        )
                                    }
                                },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                    selectedContainerColor = LightPrimary,
                                    selectedLabelColor = LightOnPrimary,
                                ),
                                border = null,
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Ana eylem: en yakın müsait araca kamerayı taşır ve detayını açar.
                    Button(
                        onClick = onFindNearest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightPrimary,
                            contentColor = LightOnPrimary,
                        ),
                    ) {
                        Icon(
                            imageVector = RencarIcons.MapPin,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "En Yakın Aracı Bul",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

/** Küçük sürükleme tutamacı çubuğu (aç/kapa göstergesi). */
@Composable
private fun HandleBar() {
    Box(
        modifier = Modifier
            .size(width = 32.dp, height = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                shape = CircleShape,
            ),
    )
}

/**
 * Alt satır metnini kurar: "Kadıköy çevresinde · ~3 dk uzaklıkta". Mahalle (cihaz Geocoder)
 * ya da mesafe eksikse ilgili parça atlanır; ikisi de yoksa null (satır gizlenir).
 */
private fun buildSubtitle(localityName: String?, nearestDistanceMeters: Float?): String? {
    val parts = mutableListOf<String>()
    if (!localityName.isNullOrBlank()) parts += "$localityName çevresinde"
    if (nearestDistanceMeters != null) {
        val meters = nearestDistanceMeters.roundToInt()
        parts += if (meters < WALK_METERS_PER_MIN) {
            "hemen yanında"
        } else {
            "~${ceil(nearestDistanceMeters / WALK_METERS_PER_MIN).toInt()} dk uzaklıkta"
        }
    }
    return parts.joinToString(" · ").ifBlank { null }
}

@Preview(name = "BottomCard · Dark", showBackground = true)
@Composable
private fun MapBottomCardDarkPreview() {
    RenCarTheme(darkTheme = true) {
        MapBottomCard(
            availableCount = 12,
            localityName = "Kadıköy",
            nearestDistanceMeters = 240f,
            selectedSegment = null,
            expanded = true,
            onToggle = {},
            onSegmentSelected = {},
            onFindNearest = {},
        )
    }
}

@Preview(name = "BottomCard · Light", showBackground = true)
@Composable
private fun MapBottomCardLightPreview() {
    RenCarTheme(darkTheme = false) {
        MapBottomCard(
            availableCount = 5,
            localityName = null,
            nearestDistanceMeters = null,
            selectedSegment = "ECONOMY",
            expanded = false,
            onToggle = {},
            onSegmentSelected = {},
            onFindNearest = {},
        )
    }
}
