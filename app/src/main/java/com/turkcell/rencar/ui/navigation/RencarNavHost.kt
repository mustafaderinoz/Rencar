package com.turkcell.rencar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.turkcell.rencar.ui.MainViewModel
import com.turkcell.rencar.ui.activerental.ActiveRentalScreen
import com.turkcell.rencar.ui.login.LoginScreen
import com.turkcell.rencar.ui.home.HomeScreen
import com.turkcell.rencar.ui.license.LicenseScreen
import com.turkcell.rencar.ui.licensepending.LicensePendingScreen
import com.turkcell.rencar.ui.onboarding.OnboardingScreen
import com.turkcell.rencar.ui.otp.OtpVerificationScreen
import com.turkcell.rencar.ui.payment.PaymentScreen
import com.turkcell.rencar.ui.rentalphotos.RentalPhotosScreen
import com.turkcell.rencar.ui.reservation.RentalPlan
import com.turkcell.rencar.ui.reservation.ReservationScreen
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
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    // Sert logout (refresh de öldü → SessionManager oturumu kapattı): tüm backstack temizlenip
    // Login'e dönülür. Sessiz token yenileme başarılı olduğunda bu olay HİÇ yayınlanmaz.
    LaunchedEffect(Unit) {
        mainViewModel.forcedLogout.collect {
            navController.navigate(RencarDestinations.LOGIN) {
                popUpTo(RencarDestinations.ONBOARDING) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

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
                // PENDING + yüklenmemiş/reddedilmiş → ehliyet doğrulama; incelemede → bekleme ekranı;
                // onaylı (CUSTOMER/ADMIN veya APPROVED) → Home. Her durumda OTP öncesi akış
                // (onboarding→login→otp) geri yığından temizlenir.
                onNavigateToLicense = {
                    navController.navigate(RencarDestinations.LICENSE) {
                        popUpTo(RencarDestinations.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateToLicensePending = {
                    navController.navigate(RencarDestinations.LICENSE_PENDING) {
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
                // Yükleme başarılı → ehliyet UNDER_REVIEW; kullanıcı bekleme ekranına kilitlenir.
                // Tüm önceki akış (onboarding→…→selfie) geri yığından temizlenir.
                onNavigateToPending = {
                    navController.navigate(RencarDestinations.LICENSE_PENDING) {
                        popUpTo(RencarDestinations.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        // Ehliyet bekleme: incelemedeki (UNDER_REVIEW) kullanıcı buraya kilitlenir (çıkılamaz).
        composable(RencarDestinations.LICENSE_PENDING) {
            LicensePendingScreen()
        }
        // Home: Harita/Geçmiş/Cüzdan/Profil sekmelerini barındıran alt navigasyonlu kabuk.
        // Araç detayındaki "Rezerve Et" → rezervasyon onayı (Home kabuğunun üstünde tam ekran).
        composable(RencarDestinations.HOME) {
            HomeScreen(
                onNavigateToReservation = { vehicleId ->
                    navController.navigate(RencarDestinations.reservationRoute(vehicleId))
                },
            )
        }
        // Rezervasyon onayı: vehicleId path argümanını taşır (ReservationViewModel SavedStateHandle ile okur).
        composable(
            route = RencarDestinations.RESERVATION_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.RESERVATION_ARG_VEHICLE_ID) { type = NavType.StringType },
            ),
        ) {
            ReservationScreen(
                onNavigateBack = { navController.popBackStack() },
                // POST /reservations başarılı → plan'a göre ilerle: Dakikalık/Saatlik'te kiralama
                // öncesi araç fotoğraf ekranı; DAILY'de foto adımı yok (API anında ACTIVE), Home'a dön.
                onReserved = { vehicleId, plan ->
                    if (plan == RentalPlan.DAILY) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(
                            RencarDestinations.rentalPhotosRoute(vehicleId, plan.apiPlan),
                        ) {
                            // Rezervasyon onayı geri yığından çıkar: foto ekranından geri → Home.
                            popUpTo(RencarDestinations.RESERVATION_ROUTE) { inclusive = true }
                        }
                    }
                },
            )
        }
        // Araç durumu (kiralama öncesi fotoğraf): vehicleId + plan path argümanını taşır
        // (RentalPhotosViewModel SavedStateHandle ile okur; açılışta POST /rentals çağırır).
        composable(
            route = RencarDestinations.RENTAL_PHOTOS_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.RENTAL_PHOTOS_ARG_VEHICLE_ID) { type = NavType.StringType },
                navArgument(RencarDestinations.RENTAL_PHOTOS_ARG_PLAN) { type = NavType.StringType },
            ),
        ) {
            RentalPhotosScreen(
                onNavigateBack = { navController.popBackStack() },
                // "Kiralamayı Başlat" (POST /rentals/{id}/start başarılı) → Aktif Yolculuk ekranı.
                // Foto ekranı geri yığından çıkar: aktif yolculuktan geri → Home.
                onStart = { rentalId ->
                    navController.navigate(RencarDestinations.activeRentalRoute(rentalId)) {
                        popUpTo(RencarDestinations.RENTAL_PHOTOS_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        // Aktif Yolculuk: rentalId path argümanını taşır (ActiveRentalViewModel SavedStateHandle ile
        // okur). GET /rentals/active poll + Socket.IO canlı konum; "Kiralamayı Bitir" → POST finish.
        composable(
            route = RencarDestinations.ACTIVE_RENTAL_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.ACTIVE_RENTAL_ARG_RENTAL_ID) { type = NavType.StringType },
            ),
        ) {
            ActiveRentalScreen(
                onNavigateBack = { navController.popBackStack() },
                // "Kiralamayı Bitir" (POST /rentals/{id}/finish) başarılı → Ödeme ekranı. Aktif yolculuk
                // geri yığından çıkar: ödemeden geri → Home (finish tek yönlüdür).
                onNavigateToPayment = { rentalId ->
                    navController.navigate(RencarDestinations.paymentRoute(rentalId)) {
                        popUpTo(RencarDestinations.ACTIVE_RENTAL_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        // Ödeme: rentalId path argümanını taşır (PaymentViewModel SavedStateHandle ile okur). GET
        // /rentals/{id} (döküm) + GET /cards + GET /wallet; POST /rentals/{id}/pay ile öder.
        composable(
            route = RencarDestinations.PAYMENT_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.PAYMENT_ARG_RENTAL_ID) { type = NavType.StringType },
            ),
        ) {
            PaymentScreen(
                onNavigateBack = { navController.popBackStack() },
                // Ödeme başarılı → tüm kiralama akışı temizlenip Home'a (harita) dönülür.
                onNavigateToHome = {
                    navController.navigate(RencarDestinations.HOME) {
                        popUpTo(RencarDestinations.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
    }
}
