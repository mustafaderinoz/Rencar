package com.turkcell.rencar.ui.rentalphotos

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.theme.rencar
import java.io.File

/** Marka mavisi — tema-bağımsız (bkz. Login/OTP/License). */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5): kamera akışı burada, sonuç intent'le VM'e gider ──
@Composable
fun RentalPhotosScreen(
    onNavigateBack: () -> Unit,
    onStart: (rentalId: String) -> Unit,
    viewModel: RentalPhotosViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Kamera akışı Android API'leriyle burada (ekran katmanı) yürür; sonuç intent'le VM'e gider.
    // rememberSaveable: kamera activity'si önlemdeyken host Activity yeniden yaratılırsa (emülatör /
    // "aktiviteleri tutma" ayarı) düz remember sıfırlanıp sonuç düşerdi; bu şekilde yön korunur.
    var pendingSide by rememberSaveable { mutableStateOf<PhotoSide?>(null) }

    fun fileFor(side: PhotoSide): File {
        val dir = File(context.filesDir, "rental-photos").apply { mkdirs() }
        return File(dir, side.fileName)
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val side = pendingSide
        if (side != null) {
            // Emülatör kamerası (ve bazı cihazlar) dosyayı yazsa da success=false döndürebiliyor;
            // dosyanın gerçekten yazıldığını doğrulayarak "foto eklenmiyor" durumunu önlüyoruz.
            val file = fileFor(side)
            if (success || file.length() > 0L) {
                viewModel.onIntent(RentalPhotosIntent.PhotoCaptured(side, file.absolutePath))
            }
        }
        pendingSide = null
    }

    fun launchCapture(side: PhotoSide) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            fileFor(side),
        )
        pendingSide = side
        takePicture.launch(uri)
    }

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val side = pendingSide
        if (granted && side != null) launchCapture(side) else pendingSide = null
    }

    fun onCaptureRequested(side: PhotoSide) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchCapture(side)
        } else {
            pendingSide = side
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    // Yolculuk başladı (ACTIVE) → Aktif Yolculuk ekranına geç (rentalId ile), bayrağı tüket.
    LaunchedEffect(uiState.started) {
        if (uiState.started) {
            uiState.rentalId?.let { onStart(it) }
            viewModel.onIntent(RentalPhotosIntent.StartedHandled)
        }
    }

    RentalPhotosScreen(
        uiState = uiState,
        // Kamera tetikleyicisi (Android API) §4.5 istisnasıdır: sonuç PhotoCaptured intent'iyle VM'e
        // döner; buradaki callback yalnız launcher'ı başlatır (state değiştirmez).
        onCapture = ::onCaptureRequested,
        onIntent = { intent ->
            when (intent) {
                RentalPhotosIntent.BackClicked -> onNavigateBack()
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5): uiState + onIntent (+ kamera tetikleyici istisnası) ──
@Composable
private fun RentalPhotosScreen(
    uiState: RentalPhotosUiState,
    onCapture: (PhotoSide) -> Unit,
    onIntent: (RentalPhotosIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .systemBarsPadding(),
    ) {
        TopBar(onBack = { onIntent(RentalPhotosIntent.BackClicked) })

        when {
            uiState.isCreating -> LoadingState(Modifier.weight(1f))
            uiState.createError != null && uiState.rentalId == null ->
                ErrorState(
                    message = uiState.createError,
                    onRetry = { onIntent(RentalPhotosIntent.Retry) },
                    modifier = Modifier.weight(1f),
                )

            else -> Content(
                uiState = uiState,
                onCapture = onCapture,
                onStartClicked = { onIntent(RentalPhotosIntent.StartClicked) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ── Üst başlık: ‹ geri + "Araç durumu" / "Başlamadan önce 4 yönü çek" ──
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
                text = "Araç durumu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Başlamadan önce 4 yönü çek",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ── İçerik: araç satırı + sayaç, 2×2 yön ızgarası, uyarı, "Kiralamayı Başlat" ──
@Composable
private fun Content(
    uiState: RentalPhotosUiState,
    onCapture: (PhotoSide) -> Unit,
    onStartClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        // Araç + sayaç satırı: "Renault Clio · 34 RNC 022"  ···  "2 / 4 çekildi"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = vehicleLine(uiState),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${uiState.uploadedCount} / ${uiState.totalSides} çekildi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = RencarBlue,
            )
        }

        Spacer(Modifier.height(16.dp))

        // 2×2 ızgara: Ön / Arka  —  Sol / Sağ
        val sides = PhotoSide.entries
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SideCell(uiState, sides[0], onCapture, Modifier.weight(1f))
            SideCell(uiState, sides[1], onCapture, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            SideCell(uiState, sides[2], onCapture, Modifier.weight(1f))
            SideCell(uiState, sides[3], onCapture, Modifier.weight(1f))
        }

        // Tek foto yükleme hatası (varsa).
        uiState.errorMessage?.let {
            Spacer(Modifier.height(14.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.weight(1f))

        WarningBanner()
        Spacer(Modifier.height(14.dp))
        StartButton(
            enabled = uiState.canStart,
            isStarting = uiState.isStarting,
            remainingCount = uiState.remainingCount,
            onStart = onStartClicked,
        )
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Tek yön kartı. Yüklüyse yeşil zeminli + araç silüeti + onay rozeti; yüklenirken spinner;
 * boşken kesikli çerçeveli "Fotoğraf çek" alanı. Her kartın sol üstünde yön etiketi çipi.
 */
@Composable
private fun SideCell(
    uiState: RentalPhotosUiState,
    side: PhotoSide,
    onCapture: (PhotoSide) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUploaded = side in uiState.uploadedSides
    val isUploading = uiState.uploadingSide == side

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isUploaded) {
                    Modifier.background(MaterialTheme.rencar.successContainer.copy(alpha = 0.55f))
                } else {
                    Modifier
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .dashedBorder(MaterialTheme.colorScheme.outlineVariant, cornerRadius = 16f)
                        .clickable(enabled = !isUploading) { onCapture(side) }
                },
            ),
    ) {
        // Sol üst: yön etiketi çipi.
        SideLabelChip(
            label = side.label,
            onSuccess = isUploaded,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )

        when {
            isUploaded -> {
                // Sağ üst: yeşil onay rozeti.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(26.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.rencar.success),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = RencarIcons.Check,
                        contentDescription = "Yüklendi",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
                // Merkez: soluk araç silüeti.
                Icon(
                    imageVector = RencarIcons.Car,
                    contentDescription = null,
                    tint = MaterialTheme.rencar.onSuccessContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp),
                )
            }

            isUploading -> CircularProgressIndicator(
                color = RencarBlue,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp),
            )

            else -> {
                // Boş durum: mavi kamera butonu + "Fotoğraf çek".
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RencarBlue)
                            .clickable { onCapture(side) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = RencarIcons.Camera,
                            contentDescription = "${side.label} fotoğraf çek",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Fotoğraf çek",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SideLabelChip(label: String, onSuccess: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (onSuccess) MaterialTheme.rencar.onSuccessContainer
        else MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (onSuccess) MaterialTheme.rencar.success.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

// ── Uyarı banner'ı: "Hasarları net çek — teslim sonrası anlaşmazlığı önler." ──
@Composable
private fun WarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.rencar.warningContainer.copy(alpha = 0.5f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = RencarIcons.Info,
            contentDescription = null,
            tint = MaterialTheme.rencar.warning,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Hasarları net çek — teslim sonrası anlaşmazlığı önler.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Alt sabit buton: "Kiralamayı Başlat" (tamamlanana dek "· N foto kaldı") ──
@Composable
private fun StartButton(
    enabled: Boolean,
    isStarting: Boolean,
    remainingCount: Int,
    onStart: () -> Unit,
) {
    Button(
        onClick = onStart,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RencarBlue,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (isStarting) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(
                text = if (enabled) "Kiralamayı Başlat"
                else "Kiralamayı Başlat · $remainingCount foto kaldı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
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

/** "Renault Clio · 34 RNC 022"; alanlar boşsa nokta ayırıcı atlanır. */
private fun vehicleLine(uiState: RentalPhotosUiState): String =
    listOf(uiState.vehicleTitle, uiState.vehiclePlate)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

/** Kesikli yuvarlak-köşe çerçeve (boş yükleme kartı için — License ile aynı). */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Float): Modifier = drawBehind {
    val stroke = Stroke(
        width = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
    )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.dp.toPx(), cornerRadius.dp.toPx()),
    )
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
private val PreviewState = RentalPhotosUiState(
    isCreating = false,
    rentalId = "clx0rent1234567890",
    vehicleTitle = "Renault Clio",
    vehiclePlate = "34 RNC 022",
    uploadedSides = setOf(PhotoSide.FRONT, PhotoSide.BACK),
    uploadedCount = 2,
    photosComplete = false,
)

@Preview(name = "RentalPhotos · Light", showBackground = true, heightDp = 760)
@Composable
private fun RentalPhotosLightPreview() {
    RenCarTheme(darkTheme = false) {
        RentalPhotosScreen(uiState = PreviewState, onCapture = {}, onIntent = {})
    }
}

@Preview(name = "RentalPhotos · Dark", showBackground = true, heightDp = 760)
@Composable
private fun RentalPhotosDarkPreview() {
    RenCarTheme(darkTheme = true) {
        RentalPhotosScreen(
            uiState = PreviewState.copy(
                uploadedSides = PhotoSide.entries.toSet(),
                uploadedCount = 4,
                photosComplete = true,
            ),
            onCapture = {},
            onIntent = {},
        )
    }
}
