package com.turkcell.rencar.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Marka mavisi (Rencar Blue, 0xFF1A6BF0) — buton/odak border/bağlantı her iki temada
 * da AYNI mavidir; `colorScheme.primary` koyu temada pastele döndüğü için tema-bağımsız
 * marka token'ı (LightPrimary) kullanılır (bkz. Onboarding).
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LoginScreen(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                LoginIntent.BackClicked -> onNavigateBack()
                LoginIntent.SendCodeClicked -> onNavigateToOtp(uiState.phone)
                // RegisterClicked: kayıt ekranı henüz yok (§2.2) — VM'de no-op olarak kalır.
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun LoginScreen(
    uiState: LoginUiState,
    onIntent: (LoginIntent) -> Unit,
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

            // ── Geri butonu (yuvarlak-köşe kare) ──
            BackButton(onClick = { onIntent(LoginIntent.BackClicked) })

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Tekrar hoş geldin",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Telefon numaranı gir, SMS ile doğrulama\nkodu gönderelim.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Telefon numarası",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(10.dp))

            // ── Ülke kodu pill + telefon alanı ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountryCodeBox()
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = uiState.phone,
                    onValueChange = { onIntent(LoginIntent.PhoneChanged(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "532 000 00 00",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = PhoneVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RencarBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        cursorColor = RencarBlue,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            }

            Spacer(Modifier.height(14.dp))

            // ── Bilgi satırı ──
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = RencarIcons.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "6 haneli kodu bu numaraya göndereceğiz. SMS ücreti operatörüne bağlıdır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── "Kod Gönder" — mavi buton + yumuşak mavi ışıma ──
            Button(
                onClick = { onIntent(LoginIntent.SendCodeClicked) },
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
                Icon(
                    imageVector = RencarIcons.ChatBubble,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Kod Gönder",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(Modifier.weight(1f))

            // ── Alt bağlantı ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hesabın yok mu? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Kayıt ol",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RencarBlue,
                    modifier = Modifier.clickable { onIntent(LoginIntent.RegisterClicked) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Geri butonu: yumuşak yüzeyli yuvarlak-köşe kare ──
@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
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

// ── Ülke kodu kutusu: "TR  +90" ──
@Composable
private fun CountryCodeBox() {
    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "TR",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "+90",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Telefon girişini "5XX XXX XX XX" biçiminde gruplar (yalnızca görsel; state saf rakam).
 * Boşluklar 3., 6. ve 8. rakamdan sonra eklenir.
 */
private val PhoneVisualTransformation = VisualTransformation { text ->
    val digits = text.text.take(10)
    val out = buildString {
        digits.forEachIndexed { i, c ->
            append(c)
            if (i == 2 || i == 5 || i == 7) append(' ')
        }
    }
    val mapping = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            var add = 0
            if (offset > 2) add++
            if (offset > 5) add++
            if (offset > 7) add++
            return offset + add
        }

        override fun transformedToOriginal(offset: Int): Int {
            var sub = 0
            if (offset > 3) sub++
            if (offset > 7) sub++
            if (offset > 10) sub++
            return (offset - sub).coerceIn(0, digits.length)
        }
    }
    TransformedText(AnnotatedString(out), mapping)
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
@Preview(name = "Login · Light", showBackground = true)
@Composable
private fun LoginScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        LoginScreen(uiState = LoginUiState(), onIntent = {})
    }
}

@Preview(name = "Login · Dark", showBackground = true)
@Composable
private fun LoginScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        LoginScreen(uiState = LoginUiState(phone = "5320000000"), onIntent = {})
    }
}
