package com.turkcell.rencar.ui.license

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.components.VerificationStep
import com.turkcell.rencar.ui.components.VerificationStepper
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.LightSuccess
import com.turkcell.rencar.ui.theme.RenCarTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Marka mavisi — tema-bağımsız (bkz. Login/OTP). */
private val RencarBlue = LightPrimary

/** Çekilen yüz (kamera hedefi). Ekran-içi UI kavramı. */
private enum class LicenseSide(val fileName: String) {
    FRONT("front.jpg"),
    BACK("back.jpg"),
}

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun LicenseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSelfie: (frontPath: String, backPath: String) -> Unit,
    viewModel: LicenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Kamera akışı Android API'leriyle burada (ekran katmanı) yürür; sonuç intent'le VM'e gider.
    // rememberSaveable: kamera activity'si önlemdeyken host Activity yeniden yaratılırsa (emülatör /
    // "aktiviteleri tutma" ayarı) düz remember sıfırlanıp sonuç düşerdi; bu şekilde yön korunur.
    var pendingSide by rememberSaveable { mutableStateOf<LicenseSide?>(null) }

    fun fileFor(side: LicenseSide): File {
        val dir = File(context.filesDir, "licenses").apply { mkdirs() }
        return File(dir, side.fileName)
    }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val side = pendingSide
        if (side != null) {
            // Emülatör kamerası (ve bazı cihazlar) dosyayı yazsa da success=false döndürebiliyor;
            // success'e körü körüne güvenmek yerine dosyanın gerçekten yazıldığını doğruluyoruz.
            val file = fileFor(side)
            if (success || file.length() > 0L) {
                val path = file.absolutePath
                when (side) {
                    LicenseSide.FRONT -> viewModel.onIntent(LicenseIntent.FrontCaptured(path))
                    LicenseSide.BACK -> viewModel.onIntent(LicenseIntent.BackCaptured(path))
                }
            }
        }
        pendingSide = null
    }

    fun launchCapture(side: LicenseSide) {
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

    fun onCaptureRequested(side: LicenseSide) {
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

    // İkisi de çekildi → selfie ekranına geç, bayrağı tüket (tekrar geçişi önler).
    LaunchedEffect(uiState.proceed) {
        if (uiState.proceed) {
            onNavigateToSelfie(uiState.frontPath.orEmpty(), uiState.backPath.orEmpty())
            viewModel.onIntent(LicenseIntent.ProceedHandled)
        }
    }

    LicenseScreen(
        uiState = uiState,
        // Kamera tetikleyicileri (Android API) §4.5 istisnasıdır: sonuç FrontCaptured/BackCaptured
        // intent'iyle VM'e döner; buradaki callback yalnız launcher'ı başlatır (state değiştirmez).
        onCaptureFront = { onCaptureRequested(LicenseSide.FRONT) },
        onCaptureBack = { onCaptureRequested(LicenseSide.BACK) },
        onIntent = { intent ->
            when (intent) {
                LicenseIntent.BackClicked -> onNavigateBack()
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5): uiState + onIntent (+ kamera tetikleyici istisnaları) ──
@Composable
private fun LicenseScreen(
    uiState: LicenseUiState,
    onCaptureFront: () -> Unit,
    onCaptureBack: () -> Unit,
    onIntent: (LicenseIntent) -> Unit,
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
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Başlık satırı: geri + başlık/alt başlık ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackButton(onClick = { onIntent(LicenseIntent.BackClicked) })
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Ehliyet doğrulama",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Kiralamadan önce tek seferlik",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            VerificationStepper(currentStep = VerificationStep.LICENSE)

            Spacer(Modifier.height(28.dp))

            // ── Ehliyet ön yüz ──
            Text(
                text = "Ehliyet ön yüz",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LicenseSideCard(
                path = uiState.frontPath,
                emptyLabel = "Ön yüzü çek veya yükle",
                onClick = onCaptureFront,
            )

            Spacer(Modifier.height(20.dp))

            // ── Ehliyet arka yüz ──
            Text(
                text = "Ehliyet arka yüz",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            LicenseSideCard(
                path = uiState.backPath,
                emptyLabel = "Arka yüzü çek veya yükle",
                onClick = onCaptureBack,
            )

            Spacer(Modifier.height(20.dp))

            // ── Bilgi banner'ı ──
            InfoBanner()

            Spacer(Modifier.weight(1f))

            // ── "Devam Et" — mavi buton ──
            Button(
                onClick = { onIntent(LicenseIntent.ContinueClicked) },
                enabled = uiState.canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = RencarBlue,
                        ambientColor = RencarBlue,
                    ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RencarBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Devam Et",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Tek bir ehliyet yüzü kartı. Boşken kesikli çerçeveli "çek veya yükle" alanı; dolu ise
 * çekilen fotoğrafın önizlemesi + yeşil "Yüklendi" rozeti (dokununca yeniden çekilir).
 */
@Composable
private fun LicenseSideCard(
    path: String?,
    emptyLabel: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = if (path != null) rememberScaledBitmap(path) else null
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = emptyLabel,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // "Yüklendi" rozeti (sağ üst).
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(LightSuccess)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = RencarIcons.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Yüklendi",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        } else {
            // Boş durum: kesikli çerçeve + kamera ikonu + etiket.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .dashedBorder(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        cornerRadius = 16.dp.value,
                    )
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = RencarBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = RencarIcons.Camera,
                            contentDescription = null,
                            tint = RencarBlue,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = emptyLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(RencarBlue.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = RencarIcons.Info,
            contentDescription = null,
            tint = RencarBlue,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = buildAnnotatedString {
                append("Bilgilerin güvenli saklanır. Doğrulama genelde ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = RencarBlue)) {
                    append("birkaç dakika")
                }
                append(" sürer.")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Geri butonu: yumuşak yüzeyli yuvarlak-köşe kare (Login/OTP ile aynı) ──
@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = RencarIcons.ChevronLeft,
            contentDescription = "Geri",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** Kesikli yuvarlak-köşe çerçeve (boş yükleme kartı için). */
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

/** Dosya yolundan bellek-güvenli (örneklemeli) bir [ImageBitmap] yükler; yüklenene dek null. */
@Composable
private fun rememberScaledBitmap(path: String): ImageBitmap? {
    var bitmap by remember(path) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(path) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(path, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
    return bitmap
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
@Preview(name = "License · Light", showBackground = true)
@Composable
private fun LicenseScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        LicenseScreen(
            uiState = LicenseUiState(),
            onCaptureFront = {},
            onCaptureBack = {},
            onIntent = {},
        )
    }
}

@Preview(name = "License · Dark", showBackground = true)
@Composable
private fun LicenseScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        LicenseScreen(
            uiState = LicenseUiState(frontPath = "/dev/null", canContinue = false),
            onCaptureFront = {},
            onCaptureBack = {},
            onIntent = {},
        )
    }
}
