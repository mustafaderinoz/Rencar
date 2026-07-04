package com.turkcell.rencar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Hilt bağımlılık grafiğinin kökü. @HiltViewModel'in çalışması için gereklidir. */
@HiltAndroidApp
class RenCarApplication : Application()
