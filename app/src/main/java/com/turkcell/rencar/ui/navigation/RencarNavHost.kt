package com.turkcell.rencar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.turkcell.rencar.ui.login.LoginScreen
import com.turkcell.rencar.ui.home.HomeScreen
import com.turkcell.rencar.ui.license.LicenseScreen
import com.turkcell.rencar.ui.onboarding.OnboardingScreen
import com.turkcell.rencar.ui.otp.OtpVerificationScreen
import com.turkcell.rencar.ui.selfie.SelfieScreen

/**
 * Uygulamanın navigasyon grafiği (decisions.md: Compose Navigation).
 *
 * Ekranlar navigasyonu yalnızca callback'ler üzerinden alır; NavController ekran
 * composable'larına veya ViewModel'lere sızmaz (tek sorumluluk / test edilebilirlik).
 */
@Composable
fun RencarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = RencarDestinations.ONBOARDING,
        // Geçiş (fade) sırasında ekranlar yarı saydamken aralarından kök/pencere arka planı
        // görünüp parlak bir "patlama"ya yol açıyordu; surface zemini bu sızıntıyı kapatır.
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Onboarding: "Hemen Başla" ve "Giriş yap" mevcut tek giriş ekranına (Login) yönlendirir.
        composable(RencarDestinations.ONBOARDING) {
            OnboardingScreen(
                onNavigateToLogin = { navController.navigate(RencarDestinations.LOGIN) },
            )
        }
        // Login: telefon girildikten sonra "Kod Gönder" ile numara OTP ekranına path argümanı olarak iletilir.
        composable(RencarDestinations.LOGIN) {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOtp = { phone ->
                    navController.navigate(RencarDestinations.otpRoute(phone))
                },
            )
        }
        // OTP doğrulama: phone path argümanını taşır (OtpVerificationViewModel SavedStateHandle ile okur).
        composable(
            route = RencarDestinations.OTP_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.OTP_ARG_PHONE) { type = NavType.StringType },
            ),
        ) {
            OtpVerificationScreen(
                onNavigateBack = { navController.popBackStack() },
                // PENDING kullanıcı → ehliyet doğrulama akışı; onaylı (CUSTOMER/ADMIN) → Home.
                // Her iki durumda da OTP öncesi akış (onboarding→login→otp) geri yığından temizlenir.
                onNavigateToLicense = {
                    navController.navigate(RencarDestinations.LICENSE) {
                        popUpTo(RencarDestinations.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(RencarDestinations.HOME) {
                        popUpTo(RencarDestinations.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        // Ehliyet doğrulama (1. adım): ön+arka çekilir, selfie ekranına yollar iletilir.
        composable(RencarDestinations.LICENSE) {
            LicenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSelfie = { front, back ->
                    navController.navigate(RencarDestinations.selfieRoute(front, back))
                },
            )
        }
        // Selfie doğrulama (2. adım): ön/arka yollarını path argümanı taşır (SelfieViewModel okur).
        composable(
            route = RencarDestinations.SELFIE_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.SELFIE_ARG_FRONT) { type = NavType.StringType },
                navArgument(RencarDestinations.SELFIE_ARG_BACK) { type = NavType.StringType },
            ),
        ) {
            SelfieScreen(
                onNavigateBack = { navController.popBackStack() },
                // Yükleme başarılı → Home. Ehliyet+selfie geri yığından temizlenir.
                onNavigateToHome = {
                    navController.navigate(RencarDestinations.HOME) {
                        popUpTo(RencarDestinations.LICENSE) { inclusive = true }
                    }
                },
            )
        }
        // Home: Harita/Geçmiş/Cüzdan/Profil sekmelerini barındıran alt navigasyonlu kabuk.
        composable(RencarDestinations.HOME) {
            HomeScreen()
        }
    }
}
