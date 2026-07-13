package com.turkcell.rencar.ui.selfie

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.components.VerificationStep
import com.turkcell.rencar.ui.components.VerificationStepper
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.LightSuccess
import com.turkcell.rencar.ui.theme.RenCarTheme
import java.util.concurrent.Executors

/** Marka mavisi — tema-bağımsız (bkz. Login/OTP). */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun SelfieScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPending: () -> Unit,
    viewModel: SelfieViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val requestCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onIntent(SelfieIntent.PermissionResult(granted)) }

    // Açılışta izni kontrol et / iste.
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onIntent(SelfieIntent.PermissionResult(true))
        } else {
            requestCamera.launch(Manifest.permission.CAMERA)
        }
    }

    SelfieScreen(
        uiState = uiState,
        onFaceStatus = { viewModel.onIntent(SelfieIntent.FaceStatusChanged(it)) },
        onBack = onNavigateBack,
        onRetry = { viewModel.onIntent(SelfieIntent.RetryClicked) },
        onDone = {
            viewModel.onIntent(SelfieIntent.DoneClicked)
            // Yükleme başarılı → ehliyet artık UNDER_REVIEW; kullanıcı bekleme ekranına kilitlenir.
            onNavigateToPending()
        },
        onGrantPermission = { requestCamera.launch(Manifest.permission.CAMERA) },
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun SelfieScreen(
    uiState: SelfieUiState,
    onFaceStatus: (FaceStatus) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDone: () -> Unit,
    onGrantPermission: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when {
            uiState.uploaded -> ApprovalContent(onDone = onDone)

            uiState.permissionGranted -> {
                CameraPreview(
                    onFaceStatus = onFaceStatus,
                    modifier = Modifier.fillMaxSize(),
                )
                ScanOverlay(uiState = uiState, modifier = Modifier.fillMaxSize())
                ScanStatusBar(
                    uiState = uiState,
                    onRetry = onRetry,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .systemBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                )
            }

            else -> PermissionContent(
                onGrantPermission = onGrantPermission,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Üst bar (Onay ekranında gizli — başlığı overlay kendi taşır).
        if (!uiState.uploaded) {
            TopBar(
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .systemBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

// ── CameraX ön kamera önizlemesi + yüz analizi ──
@Composable
private fun CameraPreview(
    onFaceStatus: (FaceStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val latestOnStatus by rememberUpdatedState(onFaceStatus)
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(analysisExecutor, FaceCenterAnalyzer { latestOnStatus(it) })
                    }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysis,
                    )
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            analysisExecutor.shutdown()
        }
    }
}

// ── Oval maske + kenarlık + ilerleme yayı + tarama çizgisi ──
@Composable
private fun ScanOverlay(
    uiState: SelfieUiState,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "scan")
    val scanFraction by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "scanLine",
    )
    val centered = uiState.faceStatus == FaceStatus.Centered
    val borderColor = if (centered) LightSuccess else Color.White.copy(alpha = 0.85f)

    Canvas(modifier = modifier) {
        val ovalW = size.width * 0.72f
        val ovalH = ovalW * 1.32f
        val left = (size.width - ovalW) / 2f
        val top = (size.height - ovalH) / 2f * 0.88f
        val rect = Rect(left, top, left + ovalW, top + ovalH)
        val path = Path().apply { addOval(rect) }

        // Oval dışını karart.
        clipPath(path, clipOp = ClipOp.Difference) {
            drawRect(Color.Black.copy(alpha = 0.55f))
        }

        // Temel kenarlık.
        drawOval(
            color = borderColor,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = 4.dp.toPx()),
        )

        // "Sabit tut" ilerleme yayı (üstten saat yönünde).
        if (uiState.holdProgress > 0f) {
            drawArc(
                color = LightSuccess,
                startAngle = -90f,
                sweepAngle = 360f * uiState.holdProgress,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // Tarama çizgisi (ortalanmadıkça ve yükleme yokken).
        if (!centered && !uiState.isUploading) {
            clipPath(path) {
                val y = rect.top + rect.height * scanFraction
                drawLine(
                    color = RencarBlue,
                    start = Offset(rect.left, y),
                    end = Offset(rect.right, y),
                    strokeWidth = 3.dp.toPx(),
                )
            }
        }
    }
}

// ── Alt durum çubuğu: yönerge / yükleniyor / hata ──
@Composable
private fun ScanStatusBar(
    uiState: SelfieUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            uiState.errorMessage != null -> {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RencarBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Tekrar dene", style = MaterialTheme.typography.titleMedium)
                }
            }

            uiState.isUploading -> {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Doğrulanıyor…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                )
            }

            else -> {
                val (title, subtitle) = when (uiState.faceStatus) {
                    FaceStatus.Centered -> "Sabit tut" to "Neredeyse tamam…"
                    FaceStatus.NotCentered -> "Yüzünü ovale ortala" to "Kamerayı yüz hizana getir."
                    FaceStatus.NoFace -> "Yüzünü çerçeveye getir" to "Selfie ile canlılık kontrolü yapıyoruz."
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Onay (3. adım) başarı içeriği ──
@Composable
private fun ApprovalContent(onDone: () -> Unit) {
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
            Spacer(Modifier.height(20.dp))
            VerificationStepper(currentStep = VerificationStep.APPROVAL)

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(color = LightSuccess.copy(alpha = 0.15f), shape = RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Check,
                    contentDescription = null,
                    tint = LightSuccess,
                    modifier = Modifier.size(44.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Ehliyetin incelemeye alındı",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Doğrulama genelde birkaç dakika sürer. Sonuç hazır olduğunda\nseni bilgilendireceğiz.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RencarBlue,
                    contentColor = Color.White,
                ),
            ) {
                Text("Bitti", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Kamera izni gerekli içeriği ──
@Composable
private fun PermissionContent(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = RencarIcons.Camera,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Kamera izni gerekli",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Selfie doğrulaması için kameraya erişmemiz gerekiyor.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onGrantPermission,
            modifier = Modifier.height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RencarBlue,
                contentColor = Color.White,
            ),
        ) {
            Text("İzin ver", style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Üst geri butonu (koyu zemin üzerinde) ──
@Composable
private fun TopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onBack() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = RencarIcons.ChevronLeft,
            contentDescription = "Geri",
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5). Kamera durumu preview'da
//    çalıştırılamayacağından izin/onay durumları render edilir. ──
@ComposePreview(name = "Selfie · İzin", showBackground = true)
@Composable
private fun SelfiePermissionPreview() {
    RenCarTheme(darkTheme = true) {
        SelfieScreen(
            uiState = SelfieUiState(permissionGranted = false, permissionRequested = true),
            onFaceStatus = {},
            onBack = {},
            onRetry = {},
            onDone = {},
            onGrantPermission = {},
        )
    }
}

@ComposePreview(name = "Selfie · Onay", showBackground = true)
@Composable
private fun SelfieApprovalPreview() {
    RenCarTheme(darkTheme = false) {
        SelfieScreen(
            uiState = SelfieUiState(uploaded = true),
            onFaceStatus = {},
            onBack = {},
            onRetry = {},
            onDone = {},
            onGrantPermission = {},
        )
    }
}
