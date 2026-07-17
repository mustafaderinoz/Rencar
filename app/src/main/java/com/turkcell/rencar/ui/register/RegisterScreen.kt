package com.turkcell.rencar.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
// androidx.hilt 1.3.0: hiltViewModel() kanonik olarak bu pakette (decisions.md).
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.components.CountryCodeBox
import com.turkcell.rencar.ui.components.PhoneVisualTransformation
import com.turkcell.rencar.ui.icons.RencarIcons
import com.turkcell.rencar.ui.theme.LightPrimary
import com.turkcell.rencar.ui.theme.RenCarTheme

/**
 * Marka mavisi — Login ile aynı tema-bağımsız token (bkz. LoginScreen/Onboarding).
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onRegistered: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // POST /auth/register başarılı → Login'e dön ve bayrağı tüket (tekrar geçişi önler).
    // Dönen token bilinçli olarak kullanılmaz; kullanıcı normal OTP akışıyla giriş yapar.
    LaunchedEffect(uiState.registered) {
        if (uiState.registered) {
            onRegistered()
            viewModel.onRegisteredHandled()
        }
    }

    RegisterScreen(
        uiState = uiState,
        onIntent = { intent ->
            when (intent) {
                // "Geri" ve "Giriş yap" aynı yere çıkar: kayıt ekranına yalnız Login'den gelinir.
                RegisterIntent.BackClicked, RegisterIntent.LoginClicked -> onNavigateBack()
                else -> viewModel.onIntent(intent)
            }
        },
    )
}

// ── Stateless gövde (§4.5) ──
@Composable
private fun RegisterScreen(
    uiState: RegisterUiState,
    onIntent: (RegisterIntent) -> Unit,
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
                .imePadding()
                // Login'den farklı olarak form uzun: klavye açıkken alanlara erişim için kaydırılır.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(12.dp))

            BackButton(onClick = { onIntent(RegisterIntent.BackClicked) })

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Hesap oluştur",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Birkaç bilgiyle hemen başla.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(32.dp))

            // ── Ad Soyad ──
            FieldLabel("Ad Soyad")
            RencarTextField(
                value = uiState.fullName,
                onValueChange = { onIntent(RegisterIntent.FullNameChanged(it)) },
                placeholder = "Deniz Yılmaz",
                error = uiState.fullNameError,
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(18.dp))

            // ── E-posta ──
            FieldLabel("E-posta")
            RencarTextField(
                value = uiState.email,
                onValueChange = { onIntent(RegisterIntent.EmailChanged(it)) },
                placeholder = "ornek@mail.com",
                error = uiState.emailError,
                keyboardType = KeyboardType.Email,
            )

            Spacer(Modifier.height(18.dp))

            // ── Şifre ──
            FieldLabel("Şifre")
            RencarTextField(
                value = uiState.password,
                onValueChange = { onIntent(RegisterIntent.PasswordChanged(it)) },
                placeholder = "En az $PASSWORD_MIN_LENGTH karakter",
                error = uiState.passwordError,
                keyboardType = KeyboardType.Password,
                visualTransformation = PasswordVisualTransformation(),
            )

            Spacer(Modifier.height(18.dp))

            // ── Telefon (Login'deki ülke kodu pill'i + biçimlendirme ile birebir) ──
            FieldLabel("Telefon numarası")
            Row(verticalAlignment = Alignment.CenterVertically) {
                CountryCodeBox()
                Spacer(Modifier.width(12.dp))
                RencarTextField(
                    value = uiState.phone,
                    onValueChange = { onIntent(RegisterIntent.PhoneChanged(it)) },
                    placeholder = "532 000 00 00",
                    // Hata metni satırın altında tek başına gösterilir (Row içinde değil).
                    error = null,
                    isError = uiState.phoneError != null,
                    keyboardType = KeyboardType.Phone,
                    visualTransformation = PhoneVisualTransformation,
                    modifier = Modifier.weight(1f),
                )
            }
            FieldError(uiState.phoneError)

            Spacer(Modifier.height(18.dp))

            // ── Davet kodu (isteğe bağlı) ──
            FieldLabel("Davet kodu (isteğe bağlı)")
            RencarTextField(
                value = uiState.referralCode,
                onValueChange = { onIntent(RegisterIntent.ReferralCodeChanged(it)) },
                placeholder = "REN-K7M2XQ",
                error = uiState.referralCodeError,
                keyboardType = KeyboardType.Text,
            )

            Spacer(Modifier.height(14.dp))

            // ── Bilgi satırı (Login'deki kalıp) ──
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = RencarIcons.Gift,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Seni davet eden biri varsa kodunu gir; ilk yolculuğunu " +
                        "tamamladığında ₺50 kazanır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── "Hesap Oluştur" — mavi buton + yumuşak mavi ışıma (Login ile aynı) ──
            Button(
                onClick = { onIntent(RegisterIntent.SubmitClicked) },
                enabled = uiState.canSubmit,
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
                        text = "Hesap Oluştur",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            // ── Alana bağlanamayan hata (ağ / sunucu doğrulaması / bilinmeyen) ──
            if (uiState.formError != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.formError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Alt bağlantı (Login'in aynadaki karşılığı) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Zaten hesabın var mı? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Giriş yap",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = RencarBlue,
                    modifier = Modifier.clickable { onIntent(RegisterIntent.LoginClicked) },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Geri butonu: yumuşak yüzeyli yuvarlak-köşe kare (Login ile aynı) ──
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

/** Alan başlığı — Login'deki "Telefon numarası" etiketiyle aynı stil. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(10.dp))
}

/** Alan altı hata metni; [message] null ise hiçbir yer kaplamaz. */
@Composable
private fun FieldError(message: String?) {
    if (message == null) return
    Spacer(Modifier.height(6.dp))
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/**
 * Login'in metin alanı görünümü (56dp yükseklik, 16dp radius, mavi odak border'ı) — formdaki beş
 * alan aynı görünümü paylaştığı için tek yerde toplandı.
 */
@Composable
private fun RencarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    // Telefon alanı ülke kodu pill'iyle Row paylaştığından Modifier.weight(1f) ile çağrılır.
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    isError: Boolean = error != null,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(16.dp),
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RencarBlue,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            errorBorderColor = MaterialTheme.colorScheme.error,
            cursorColor = RencarBlue,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
    FieldError(error)
}

// ── Preview'lar: stateless gövde, Hilt'siz, sabit state (§4.5) ──
@Preview(name = "Register · Light", showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        RegisterScreen(uiState = RegisterUiState(), onIntent = {})
    }
}

@Preview(name = "Register · Dark", showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        RegisterScreen(
            uiState = RegisterUiState(
                fullName = "Deniz Yılmaz",
                email = "deniz@mail.com",
                password = "123456",
                phone = "5320000000",
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Register · Hatalar", showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenErrorPreview() {
    RenCarTheme(darkTheme = false) {
        RegisterScreen(
            uiState = RegisterUiState(
                fullName = "Deniz Yılmaz",
                email = "deniz@mail.com",
                phone = "5320000000",
                emailError = "Bu e-posta adresi zaten kayıtlı.",
                phoneError = "Bu telefon numarası zaten kayıtlı.",
                referralCodeError = "Davet kodu geçersiz.",
            ),
            onIntent = {},
        )
    }
}
