package com.turkcell.rencar.ui.otp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Marka mavisi (Rencar Blue, 0xFF1A6BF0) — buton/odak border/bağlantı her iki temada
 * da AYNI mavidir; `colorScheme.primary` koyu temada pastele döndüğü için tema-bağımsız
 * marka token'ı (LightPrimary) kullanılır.
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun OtpVerificationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToLicense: () -> Unit,
    onNavigateToLicensePending: () -> Unit,
    viewModel: OtpVerificationViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // POST /auth/verify-otp başarılı → role + ehliyet durumuna göre hedefe git; bayrağı tüket.
    LaunchedEffect(uiState.verified) {
        if (uiState.verified) {
            when (uiState.destination) {
                PostVerifyDestination.HOME -> onNavigateToHome()
                PostVerifyDestination.LICENSE_UPLOAD -> onNavigateToLicense()
                PostVerifyDestination.LICENSE_PENDING -> onNavigateToLicensePending()
            }
            viewModel.onIntent(OtpVerificationIntent.VerifiedHandled)
        }
    }

    OtpVerificationScreen(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                OtpVerificationIntent.BackClicked -> onNavigateBack()
                // VerifyClicked artık VM'e gider (API çağrısı); geçiş verified ile tetiklenir.
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun OtpVerificationScreen(
    uiState: OtpVerificationUiState,
    onIntent: (OtpVerificationIntent) -> Unit,
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

            // ── Geri butonu ──
            BackButton(onClick = { onIntent(OtpVerificationIntent.BackClicked) })

            Spacer(Modifier.height(28.dp))

            // ── OTP Logo/Icon ──
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = RencarBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = RencarIcons.Phone,
                    contentDescription = null,
                    tint = RencarBlue,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Telefonunu doğrula",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            // ── Telefon numarası kalın, geri kalan metin gri ──
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    ) {
                        append(uiState.phoneNumber)
                    }
                    append(" numarasına\ngönderdiğimiz 6 haneli kodu gir.")
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            // ── 6 Haneli OTP Input ──
            OtpInputField(
                value = uiState.otpCode,
                onValueChange = { onIntent(OtpVerificationIntent.OtpCodeChanged(it)) },
            )

            Spacer(Modifier.height(20.dp))

            // ── Saat ikonu + "Kodu tekrar gönder · 0:42" tek satır ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = RencarIcons.Clock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Kodu tekrar gönder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onIntent(OtpVerificationIntent.ResendCodeClicked) },
                )
                Text(
                    text = " · ${uiState.timeRemaining / 60}:${String.format("%02d", uiState.timeRemaining % 60)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Hata mesajı (POST /auth/verify-otp başarısız) ──
            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── "Doğrula ve Devam Et" — mavi buton ──
            Button(
                onClick = { onIntent(OtpVerificationIntent.VerifyClicked) },
                enabled = uiState.otpCode.length == 6 && !uiState.isLoading,
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
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = "Doğrula ve Devam Et",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Alt bağlantı: "Numara yanlış mı? Değiştir" ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Numara yanlış mı? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Değiştir",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RencarBlue,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onIntent(OtpVerificationIntent.BackClicked) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * 6 haneli OTP kutuları — görünmez [BasicTextField] + `decorationBox` deseni.
 * Kutulara dokununca gerçek klavye açılır (sistem odak/tıklama akışı text field
 * üzerinden yürür), yazılan her hane ilgili kutuya dağılır; boş+aktif kutuda
 * mavi imleç çubuğu gösterilir (§4.2 saf UI kuralına uygun: state dışarıdan gelir).
 */
@Composable
private fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    BasicTextField(
        value = value,
        onValueChange = { new ->
            val digitsOnly = new.filter(Char::isDigit).take(6)
            onValueChange(digitsOnly)
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            ) {
                repeat(6) { index ->
                    OtpDigitBox(
                        digit = value.getOrNull(index)?.toString() ?: "",
                        isFocused = index == value.length,
                    )
                }
            }
        },
    )

    // Ekran açılınca klavye otomatik çıksın (fotoğraftaki gibi aktif imleç).
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

// ── Tekil OTP Digit Box ──
@Composable
private fun OtpDigitBox(
    digit: String,
    isFocused: Boolean,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) RencarBlue else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp),
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        when {
            digit.isNotEmpty() -> Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            isFocused -> Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(RencarBlue, RoundedCornerShape(1.dp)),
            )
        }
    }
}

// ── Geri Butonu (Login ekranından kopyalanan) ──
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

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
@Preview(name = "OTP · Light", showBackground = true)
@Composable
private fun OtpVerificationScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        OtpVerificationScreen(
            uiState = OtpVerificationUiState(
                phoneNumber = "+90 532 000 00 00",
                otpCode = "482",
                timeRemaining = 299,
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "OTP · Dark", showBackground = true)
@Composable
private fun OtpVerificationScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        OtpVerificationScreen(
            uiState = OtpVerificationUiState(
                phoneNumber = "+90 532 000 00 00",
                otpCode = "482",
                timeRemaining = 299,
            ),
            onIntent = {},
        )
    }
}