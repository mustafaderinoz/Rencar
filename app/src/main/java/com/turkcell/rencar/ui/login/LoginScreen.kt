package com.turkcell.rencar.ui.login

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
 * Marka mavisi (Rencar Blue, 0xFF1A6BF0) — buton/odak border/bağlantı her iki temada
 * da AYNI mavidir; `colorScheme.primary` koyu temada pastele döndüğü için tema-bağımsız
 * marka token'ı (LightPrimary) kullanılır (bkz. Onboarding).
 */
private val RencarBlue = LightPrimary

// ── Stateful sarmalayıcı (§4.5) ──
/**
 * [justRegistered]: kullanıcı kayıt ekranından yeni döndüyse true — bilgi satırı gösterilir.
 * Bu bayrak VM'de DEĞİL, Login'in NavBackStackEntry'sinde tutulur (kayıt ekranı oraya yazar);
 * entry'nin savedStateHandle'ı Hilt'in VM'e verdiği handle ile aynı nesne olmadığından NavHost
 * okuyup buraya geçirir (bkz. RencarDestinations.LOGIN_RESULT_JUST_REGISTERED).
 */
@Composable
fun LoginScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    onNavigateToRegister: (String) -> Unit,
    justRegistered: Boolean = false,
    onJustRegisteredShown: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // POST /auth/login başarılı → OTP ekranına geç ve bayrağı tüket (tekrar geçişi önler).
    LaunchedEffect(uiState.codeSent) {
        if (uiState.codeSent) {
            onNavigateToOtp(uiState.phone)
            viewModel.onIntent(LoginIntent.CodeSentHandled)
        }
    }

    // POST /auth/login → 401 (numara kayıtlı değil) → kayıt ekranına geç, numarayı taşı.
    LaunchedEffect(uiState.navigateToRegister) {
        if (uiState.navigateToRegister) {
            onNavigateToRegister(uiState.phone)
            viewModel.onIntent(LoginIntent.NavigateToRegisterHandled)
        }
    }

    LoginScreen(
        // Bayrak nav katmanından geldiğinden stateless gövdeye state'in parçası olarak verilir;
        // gövde ve preview'lar yalnız LoginUiState görür (§4.5).
        uiState = uiState.copy(justRegistered = justRegistered),
        onIntent = { intent ->
            when (intent) {
                LoginIntent.BackClicked -> onNavigateBack()
                // Alttaki "Kayıt ol" linki: numara henüz girilmemiş olabilir, girildiyse taşınır.
                LoginIntent.RegisterClicked -> onNavigateToRegister(uiState.phone)
                // SendCodeClicked artık VM'e gider (API çağrısı); geçiş codeSent ile tetiklenir.
                else -> {
                    // Kullanıcı harekete geçti → "kaydın tamamlandı" bilgisi görevini bitirdi.
                    if (justRegistered) onJustRegisteredShown()
                    viewModel.onIntent(intent)
                }
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
                enabled = uiState.phone.length == 10 && !uiState.isLoading,
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
            }

            // ── Hata mesajı (POST /auth/login başarısız; 401 hariç → kayıt akışına gider) ──
            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // ── Kayıt sonrası dönüş bilgisi: kullanıcı buraya kaydını tamamlayıp gelir ──
            if (uiState.justRegistered) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = RencarIcons.Check,
                        contentDescription = null,
                        tint = RencarBlue,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Kaydın tamamlandı. Şimdi numaranı doğrulayıp giriş yapabilirsin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
