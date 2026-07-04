package com.turkcell.rencar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.turkcell.rencar.ui.login.LoginScreen
import com.turkcell.rencar.ui.theme.RenCarTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RenCarTheme {
                // Splash/Onboarding sonrası giriş ekranı. Gerçek splash→login
                // navigasyon grafiği ayrı bir işte kurulacak (§4.6).
                LoginScreen()
            }
        }
    }
}
