package com.turkcell.rencar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turkcell.rencar.ui.MainViewModel
import com.turkcell.rencar.ui.navigation.RencarNavHost
import com.turkcell.rencar.ui.theme.RenCarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Tema tek noktadan sürülür: kullanıcı tercihi varsa o, yoksa sistem ayarı.
            // Profil ekranındaki toggle tercihi DataStore'a yazar → burası anında yeniden çizer.
            val storedDarkTheme by mainViewModel.darkTheme.collectAsStateWithLifecycle()

            RenCarTheme(darkTheme = storedDarkTheme ?: isSystemInDarkTheme()) {
                // Splash başlangıçlı uygulama navigasyon grafiği (bkz. RencarNavHost).
                RencarNavHost()
            }
        }
    }
}
