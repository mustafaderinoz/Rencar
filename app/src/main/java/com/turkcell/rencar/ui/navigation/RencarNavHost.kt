package com.turkcell.rencar.ui.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.turkcell.rencar.ui.register.RegisterScreen
import com.turkcell.rencar.ui.rentalphotos.RentalPhotosScreen
import com.turkcell.rencar.ui.rentalreturnphotos.RentalReturnPhotosScreen
import com.turkcell.rencar.ui.reservation.ReservationScreen
import com.turkcell.rencar.ui.selfie.SelfieScreen
import com.turkcell.rencar.ui.splash.SplashDestination
import com.turkcell.rencar.ui.splash.SplashScreen

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
                // Başlangıç artık SPLASH; oto-login'le doğrudan Home'a girilince geri yığında
                // ONBOARDING/SPLASH bulunmayabilir. Tüm grafiği temizleyip yalnız Login bırak.
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = RencarDestinations.SPLASH,

        // Geçiş (fade) sırasında ekranlar yarı saydamken aralarından kök/pencere arka planı
        // görünüp parlak bir "patlama"ya yol açıyordu; surface zemini bu sızıntıyı kapatır.
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Açılış (session restore): saklı token'la oturumu geri yükleyip çözülen ilk hedefe geçer;
        // splash geri yığından temizlenir (kullanıcı geri tuşuyla splash'e dönemez).
        composable(RencarDestinations.SPLASH) {
            SplashScreen(
                onDestinationResolved = { destination ->
                    val route = when (destination) {
                        SplashDestination.Onboarding -> RencarDestinations.ONBOARDING
                        SplashDestination.Login -> RencarDestinations.LOGIN
                        SplashDestination.Home -> RencarDestinations.HOME
                        SplashDestination.LicenseUpload -> RencarDestinations.LICENSE
                        SplashDestination.LicensePending -> RencarDestinations.LICENSE_PENDING
                        // Yeniden açılış kurtarma: devam eden akışın ekranına doğrudan inilir (Splash
                        // backstack'ten çıkar). Akış bittiğinde (bitir → ödeme → Home) yığın Home'a döner.
                        is SplashDestination.ActiveRental ->
                            RencarDestinations.activeRentalRoute(destination.rentalId)
                        is SplashDestination.PreparingRental ->
                            RencarDestinations.rentalPhotosRoute(destination.vehicleId, destination.plan)
                        is SplashDestination.ActiveReservation ->
                            RencarDestinations.reservationRoute(destination.vehicleId)
                    }
                    navController.navigate(route) {
                        popUpTo(RencarDestinations.SPLASH) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        // Onboarding: "Hemen Başla" ve "Giriş yap" mevcut tek giriş ekranına (Login) yönlendirir.
        composable(RencarDestinations.ONBOARDING) {
            OnboardingScreen(
                onNavigateToLogin = { navController.navigate(RencarDestinations.LOGIN) },
            )
        }
        // Login: telefon girildikten sonra "Kod Gönder" ile numara OTP ekranına path argümanı olarak iletilir.
        composable(RencarDestinations.LOGIN) { entry ->
            // Kayıt ekranı Login'e dönerken sonucu BU girdinin savedStateHandle'ına yazar; VM'e
            // enjekte edilen handle ayrı nesne olduğundan bayrak burada okunup ekrana verilir.
            val justRegistered by entry.savedStateHandle
                .getStateFlow(RencarDestinations.LOGIN_RESULT_JUST_REGISTERED, false)
                .collectAsStateWithLifecycle()

            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOtp = { phone ->
                    navController.navigate(RencarDestinations.otpRoute(phone))
                },
                // Numara kayıtlı değil (401) ya da "Kayıt ol" linki → kayıt ekranı; numara taşınır.
                onNavigateToRegister = { phone ->
                    navController.navigate(RencarDestinations.registerRoute(phone))
                },
                justRegistered = justRegistered,
                onJustRegisteredShown = {
                    entry.savedStateHandle[RencarDestinations.LOGIN_RESULT_JUST_REGISTERED] = false
                },
            )
        }
        // Kayıt: telefon İSTEĞE BAĞLI query argümanıdır (RegisterViewModel SavedStateHandle ile okur;
        // "Kayıt ol" linkinden boş gelebilir). Kayıt başarılı olduğunda API token DÖNSE DE bilinçli
        // olarak kullanılmaz (bkz. AuthRepository.register): kullanıcı Login'e döner ve normal OTP
        // akışıyla giriş yapar.
        composable(
            route = RencarDestinations.REGISTER_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.REGISTER_ARG_PHONE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onRegistered = {
                    // Login'e DÖNÜLÜR (yeniden kurulmaz): geri yığındaki girdi korunduğu için
                    // kullanıcının girdiği numara alanda kalır, hemen "Kod Gönder"e basabilir.
                    navController.getBackStackEntry(RencarDestinations.LOGIN)
                        .savedStateHandle[RencarDestinations.LOGIN_RESULT_JUST_REGISTERED] = true
                    navController.popBackStack(RencarDestinations.LOGIN, inclusive = false)
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
            // Ehliyet ekranına gelinirken auth akışı geri yığından temizlendiği için (popUpTo …
            // inclusive) çoğu senaryoda LICENSE tek girdidir; o durumda popBackStack() bir şey yapmaz.
            // Ekran-içi ‹ butonu da sistem geri tuşu gibi davranıp uygulamadan çıksın (kök gate ekranı).
            val activity = LocalActivity.current
            LicenseScreen(
                onNavigateBack = { if (!navController.popBackStack()) activity?.finish() },
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
                // Dakikalık/Saatlik: rezervasyon sonrası kiralama öncesi araç fotoğraf ekranı
                // (kiralamayı POST /rentals ile o ekran açar). DAILY buraya düşmez.
                onReserved = { vehicleId, plan ->
                    navController.navigate(
                        RencarDestinations.rentalPhotosRoute(vehicleId, plan.apiPlan),
                    ) {
                        // Rezervasyon onayı geri yığından çıkar: foto ekranından geri → Home.
                        popUpTo(RencarDestinations.RESERVATION_ROUTE) { inclusive = true }
                    }
                },
                // Günlük: foto adımı yoktur; kiralama rezervasyon ekranında açılır ve API yolculuğu
                // anında ACTIVE yapar → doğrudan Aktif Yolculuk (oradan bitir → ödeme).
                onDailyRentalStarted = { rentalId ->
                    navController.navigate(RencarDestinations.activeRentalRoute(rentalId)) {
                        popUpTo(RencarDestinations.RESERVATION_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        // Araç durumu (kiralama öncesi fotoğraf): vehicleId + plan path argümanını taşır
        // (RentalPhotosViewModel SavedStateHandle ile okur). Açılışta kiralama OLUŞTURULMAZ; aktif
        // rezervasyonun 15 dk geri sayımı gösterilir, POST /rentals ancak "Başlat"ta çağrılır.
        composable(
            route = RencarDestinations.RENTAL_PHOTOS_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.RENTAL_PHOTOS_ARG_VEHICLE_ID) { type = NavType.StringType },
                navArgument(RencarDestinations.RENTAL_PHOTOS_ARG_PLAN) { type = NavType.StringType },
            ),
        ) {
            RentalPhotosScreen(
                onNavigateBack = { navController.popBackStack() },
                // "Kiralamayı Başlat" (POST /rentals → foto upload → start başarılı) → Aktif Yolculuk.
                // Foto ekranı geri yığından çıkar: aktif yolculuktan geri → Home.
                onStart = { rentalId ->
                    navController.navigate(RencarDestinations.activeRentalRoute(rentalId)) {
                        popUpTo(RencarDestinations.RENTAL_PHOTOS_ROUTE) { inclusive = true }
                    }
                },
                // "Rezervasyonu İptal Et" (DELETE /reservations/{id}) → araç serbest; tüm akış temizlenip
                // Home'a dönülür (Ödeme başarısı kalıbıyla aynı).
                onCancelled = {
                    navController.navigate(RencarDestinations.HOME) {
                        popUpTo(RencarDestinations.HOME) { inclusive = true }
                        launchSingleTop = true
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
                // "Kiralamayı Bitir" (POST /rentals/{id}/finish) başarılı → Araç teslim durumu (foto)
                // ekranı; ödeme oradan açılır. Aktif yolculuk geri yığından çıkar (finish tek yönlüdür).
                onNavigateToReturnPhotos = { rentalId, vehicleTitle, vehiclePlate ->
                    navController.navigate(
                        RencarDestinations.rentalReturnPhotosRoute(rentalId, vehicleTitle, vehiclePlate),
                    ) {
                        popUpTo(RencarDestinations.ACTIVE_RENTAL_ROUTE) { inclusive = true }
                    }
                },
            )
        }
        // Araç teslim durumu (kiralama sonrası fotoğraf): rentalId path + araç özeti query argümanı
        // (RentalReturnPhotosViewModel SavedStateHandle ile okur). Fotoğraflar yalnız cihazda tutulur
        // (teslim-foto ucu backend'de yok — §2.2, mock); ağ çağrısı yapılmaz.
        composable(
            route = RencarDestinations.RENTAL_RETURN_PHOTOS_ROUTE,
            arguments = listOf(
                navArgument(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_RENTAL_ID) {
                    type = NavType.StringType
                },
                navArgument(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_VEHICLE_TITLE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(RencarDestinations.RENTAL_RETURN_PHOTOS_ARG_VEHICLE_PLATE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            RentalReturnPhotosScreen(
                onNavigateBack = { navController.popBackStack() },
                // 4 yön tamam → Ödeme ekranı. Foto ekranı geri yığından çıkar: ödemeden geri → Home.
                onContinueToPayment = { rentalId ->
                    navController.navigate(RencarDestinations.paymentRoute(rentalId)) {
                        popUpTo(RencarDestinations.RENTAL_RETURN_PHOTOS_ROUTE) { inclusive = true }
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
