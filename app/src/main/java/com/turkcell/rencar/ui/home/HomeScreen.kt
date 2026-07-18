package com.turkcell.rencar.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.turkcell.rencar.ui.map.MapScreen
import com.turkcell.rencar.ui.navigation.RencarDestinations
import com.turkcell.rencar.ui.profile.ProfileScreen
import com.turkcell.rencar.ui.rentals.RentalsScreen
import com.turkcell.rencar.ui.theme.RenCarTheme
import com.turkcell.rencar.ui.wallet.WalletScreen

/**
 * Alt navigasyonlu ana kabuk: [Scaffold] + [RencarBottomBar] + sekmelere özel nested
 * [NavHost]. Sekmeye basınca ilgili rotaya geçilir; sekme durumları (state) korunur.
 *
 * Her sekme kendi `ui/<feature>/` MVI ekranına bağlıdır (Harita/Geçmiş/Cüzdan/Profil).
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToReservation: (vehicleId: String) -> Unit = {},
    tabNavController: NavHostController = rememberNavController(),
) {
    val backStackEntry by tabNavController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            RencarBottomBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    // Sekmeler arası standart geçiş: tek örnek + durum sakla/geri yükle.
                    tabNavController.navigate(tab.route) {
                        popUpTo(tabNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = tabNavController,
            startDestination = RencarDestinations.MAP,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(RencarDestinations.MAP) {
                MapScreen(onNavigateToReservation = onNavigateToReservation)
            }
            composable(RencarDestinations.HISTORY) { RentalsScreen() }
            composable(RencarDestinations.WALLET) { WalletScreen() }
            composable(RencarDestinations.PROFILE) { ProfileScreen() }
        }
    }
}

@Preview(name = "Home · Dark", showBackground = true)
@Composable
private fun HomeScreenDarkPreview() {
    RenCarTheme(darkTheme = true) {
        HomeScreen()
    }
}

@Preview(name = "Home · Light", showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    RenCarTheme(darkTheme = false) {
        HomeScreen()
    }
}
