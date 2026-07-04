# RenCar - Renk Sistemi

> Bu dosya RenCar isimli araç kiralama uygulamasının renk paleti için
> **tek doğruluk kaynağıdır** (single source of truth) ve doğrudan bir
> **Android Jetpack Compose** projesinde kullanılmak üzere düzenlenmiştir.

---

## 1. Temel Kural

> Hiçbir `@Composable` içinde ham `Color(0xFF..)` yazılmaz.
> Renkler daima `MaterialTheme.colorScheme.<slot>` (veya RenCar'a özel
> renkler için `MaterialTheme.rencar.<slot>`) üzerinden okunmak zorundadır.

Ham `Color(..)` tanımı **yalnızca** `Color.kt` içinde, sabit değişken
tanımlanırken kullanılır. Marka mavisi, semantik renkler ve harita/araç
kategori renkleri de bu kurala tabidir.

---

## 2. `Color.kt` — Ham Token Tanımları

Tasarımdaki iki tema (açık + koyu) için Material 3 slot seti aşağıdadır.
`// türetilen (bkz. §5)` işaretli değerler tohum (seed) mavisinden
üretilmiştir, tasarımdan birebir okunmamıştır.

```kotlin
package com.rencar.ui.theme

import androidx.compose.ui.graphics.Color

// ── DARK ──
val DarkPrimary            = Color(0xFFADC6FF)
val DarkOnPrimary          = Color(0xFF002E6A)
val DarkPrimaryContainer   = Color(0xFF00489C)
val DarkOnPrimaryContainer = Color(0xFFD8E4FF)

val DarkSecondary            = Color(0xFFBCC7DC)
val DarkOnSecondary          = Color(0xFF263141)
val DarkSecondaryContainer   = Color(0xFF3D4758)
val DarkOnSecondaryContainer = Color(0xFFD8E3F9)

val DarkTertiary            = Color(0xFF4CD9DF) // Elektrikli aksanı (teal)
val DarkOnTertiary          = Color(0xFF00373A)
val DarkTertiaryContainer   = Color(0xFF004F53)
val DarkOnTertiaryContainer = Color(0xFF6FF6FB)

val DarkError              = Color(0xFFFFB4AB)
val DarkOnError            = Color(0xFF690005)
val DarkErrorContainer     = Color(0xFF93000A) // türetilen (bkz. §5)
val DarkOnErrorContainer   = Color(0xFFFFDAD6) // türetilen (bkz. §5)

val DarkSurface                  = Color(0xFF0C0E11) // tasarım koyu zemin (~#0A0A0B)
val DarkSurfaceDim               = Color(0xFF0C0E11)
val DarkSurfaceBright            = Color(0xFF32353A)
val DarkSurfaceContainerLowest   = Color(0xFF070709)
val DarkSurfaceContainerLow      = Color(0xFF151517) // tasarım kart zemini
val DarkSurfaceContainer         = Color(0xFF1A1C1F)
val DarkSurfaceContainerHigh     = Color(0xFF24272B) // yükseltilmiş (input/pill)
val DarkSurfaceContainerHighest  = Color(0xFF2F3236)

val DarkOnSurface         = Color(0xFFE4E2E6) // birincil metin (koyu)
val DarkOnSurfaceVariant  = Color(0xFFC4C6D0) // ikincil metin (koyu)
val DarkSurfaceVariant    = Color(0xFF44474F) // türetilen (bkz. §5)
val DarkOutline           = Color(0xFF8E9099)
val DarkOutlineVariant    = Color(0xFF44474F)
val DarkInverseSurface    = Color(0xFFE4E2E6)
val DarkInverseOnSurface  = Color(0xFF2E3135)
val DarkInversePrimary    = Color(0xFF1A6BF0) // türetilen (light/marka primary)
val DarkScrim             = Color(0xFF000000)

// ── LIGHT ──
val LightPrimary            = Color(0xFF1A6BF0) // marka mavisi (Rencar Blue)
val LightOnPrimary          = Color(0xFFFFFFFF)
val LightPrimaryContainer   = Color(0xFFD8E4FF)
val LightOnPrimaryContainer = Color(0xFF001A43)

val LightSecondary            = Color(0xFF545F71)
val LightOnSecondary          = Color(0xFFFFFFFF)
val LightSecondaryContainer   = Color(0xFFD8E3F9)
val LightOnSecondaryContainer = Color(0xFF111C2B)

val LightTertiary            = Color(0xFF00696E) // Elektrikli aksanı (teal)
val LightOnTertiary          = Color(0xFFFFFFFF)
val LightTertiaryContainer   = Color(0xFF6FF6FB)
val LightOnTertiaryContainer = Color(0xFF002022)

val LightError              = Color(0xFFBA1A1A)
val LightOnError            = Color(0xFFFFFFFF)
val LightErrorContainer     = Color(0xFFFFDAD6) // türetilen (bkz. §5)
val LightOnErrorContainer   = Color(0xFF410002) // türetilen (bkz. §5)

val LightSurface                  = Color(0xFFFBFCFE)
val LightSurfaceDim               = Color(0xFFDAD9DE)
val LightSurfaceBright            = Color(0xFFFBFCFE)
val LightSurfaceContainerLowest   = Color(0xFFFFFFFF) // tasarım kart zemini
val LightSurfaceContainerLow      = Color(0xFFF5F6F8) // tasarım uygulama zemini
val LightSurfaceContainer         = Color(0xFFEFF1F4)
val LightSurfaceContainerHigh     = Color(0xFFE9EBEF)
val LightSurfaceContainerHighest  = Color(0xFFE3E6EB)

val LightOnSurface        = Color(0xFF191C20) // birincil metin (açık)
val LightOnSurfaceVariant = Color(0xFF44474F) // ikincil metin (açık)
val LightSurfaceVariant   = Color(0xFFE0E2EC) // türetilen (bkz. §5)
val LightOutline          = Color(0xFF74777F)
val LightOutlineVariant   = Color(0xFFC4C6D0)
val LightInverseSurface   = Color(0xFF2E3135)
val LightInverseOnSurface = Color(0xFFF0F0F4)
val LightInversePrimary   = Color(0xFFADC6FF) // türetilen (dark primary)
val LightScrim            = Color(0xFF000000)
```

---

## 3. `Color.kt` — RenCar'a Özel Genişletilmiş Tokenlar

Material 3'ün standart `ColorScheme`'inde **karşılığı olmayan** ama tasarımda
zorunlu olan renkler. "Müsait / Onaylı / Yüklendi" (yeşil), hasar uyarısı
(amber) ve harita/araç kategorileri buraya girer. §4'te `RencarExtendedColors`
ile temaya bağlanır.

```kotlin
// ── SUCCESS (Müsait / Onaylı / Yakıt barı / +bakiye) ──
val LightSuccess          = Color(0xFF16A34A)
val LightOnSuccess        = Color(0xFFFFFFFF)
val LightSuccessContainer = Color(0xFFC7F2D6)
val LightOnSuccessContainer = Color(0xFF00210F)

val DarkSuccess           = Color(0xFF6BD98E)
val DarkOnSuccess         = Color(0xFF003919)
val DarkSuccessContainer  = Color(0xFF0E5228)
val DarkOnSuccessContainer = Color(0xFFC7F2D6)

// ── WARNING (Hasarları net çek uyarısı) ──
val LightWarning          = Color(0xFFF59E0B)
val LightOnWarning        = Color(0xFFFFFFFF)
val LightWarningContainer = Color(0xFFFFE2B8)
val LightOnWarningContainer = Color(0xFF2A1800)

val DarkWarning           = Color(0xFFF5B740)
val DarkOnWarning         = Color(0xFF3A2600)
val DarkWarningContainer  = Color(0xFF5A3D00)
val DarkOnWarningContainer = Color(0xFFFFE2B8)

// ── VEHICLE CATEGORY / MAP MARKER (tema bağımsız - marka kimliği) ──
val CatEconomy  = Color(0xFFF97316) // Ekonomik  🟠 ₺28
val CatComfort  = Color(0xFF7C4DFF) // Konfor     🟣 ₺38
val CatSuv      = Color(0xFFF5B301) // SUV        🟡 ₺32
val CatElectric = Color(0xFF14B8A6) // Elektrikli 🟢 ₺26
val CatBusy     = Color(0xFF64748B) // Kullanımda ⚪
```

---

## 4. `Theme.kt` — ColorScheme + Genişletilmiş Renkler

```kotlin
package com.rencar.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceDim = LightSurfaceDim,
    surfaceBright = LightSurfaceBright,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
    scrim = LightScrim,
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceDim = DarkSurfaceDim,
    surfaceBright = DarkSurfaceBright,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    scrim = DarkScrim,
)

// ── Genişletilmiş renkler (M3 dışı) ──
@Immutable
data class RencarExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val catEconomy: Color,
    val catComfort: Color,
    val catSuv: Color,
    val catElectric: Color,
    val catBusy: Color,
)

private val LightExtended = RencarExtendedColors(
    success = LightSuccess,
    onSuccess = LightOnSuccess,
    successContainer = LightSuccessContainer,
    onSuccessContainer = LightOnSuccessContainer,
    warning = LightWarning,
    onWarning = LightOnWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer,
    catEconomy = CatEconomy,
    catComfort = CatComfort,
    catSuv = CatSuv,
    catElectric = CatElectric,
    catBusy = CatBusy,
)

private val DarkExtended = RencarExtendedColors(
    success = DarkSuccess,
    onSuccess = DarkOnSuccess,
    successContainer = DarkSuccessContainer,
    onSuccessContainer = DarkOnSuccessContainer,
    warning = DarkWarning,
    onWarning = DarkOnWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer,
    catEconomy = CatEconomy,
    catComfort = CatComfort,
    catSuv = CatSuv,
    catElectric = CatElectric,
    catBusy = CatBusy,
)

val LocalRencarColors = staticCompositionLocalOf { LightExtended }

@Composable
fun RencarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtended else LightExtended

    CompositionLocalProvider(LocalRencarColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = RencarTypography, // kendi tipografiniz
            content = content,
        )
    }
}

/** Kısa erişim: MaterialTheme.rencar.success gibi. */
val MaterialTheme.rencar: RencarExtendedColors
    @Composable get() = LocalRencarColors.current
```

---

## 5. Türetilen Değerler (Derived)

Aşağıdaki tokenlar tasarımdan birebir okunmadı; Material 3 tonal mantığına
göre tohum renklerinden üretildi. Marka kaynağı (Figma) ile doğrulanmalıdır.

- **`primary` (light) = `#1A6BF0`** tasarımdaki buton mavisine kilitlenmiştir
  (marka rengi). Diğer tüm mavi slotlar bu tohumdan türetildi.
- **`errorContainer` / `onErrorContainer`** — Material 3 baseline kırmızı
  tonlarıdır (tasarımda ayrı bir hata konteyneri yoktu).
- **`surfaceVariant`, `outlineVariant`** — nötr tonal paletten türetildi.
- **`inversePrimary`** — karşı temanın `primary` değeridir
  (light ↔ `#ADC6FF`, dark ↔ `#1A6BF0`).
- **`tertiary` (teal)** — tasarımdaki "Elektrikli" kategori tonundan
  (`#14B8A6`) üretilmiş M3 tertiary ailesidir.
- **Genişletilmiş koyu tema konteynerleri** (`successContainer`,
  `warningContainer` vb.) — açık tema semantiğinden koyulaştırılarak türetildi.

---

## 6. Slot → Tasarım Kullanımı

Tasarımdaki her öğe hangi slot ile boyanır:

| Tasarım öğesi                                       | Slot                                          |
| --------------------------------------------------- | --------------------------------------------- |
| Birincil buton (Hemen Başla, Kod Gönder, Kilidi Aç) | `primary` / `onPrimary`                       |
| "Giriş yap", "Kayıt ol", "Değiştir" linkleri        | `primary`                                     |
| Seçili sekme / seçili plan kartı kenarlığı          | `primary`                                     |
| Uygulama zemini (açık)                              | `surfaceContainerLow` (`#F5F6F8`)             |
| Kart / bottom sheet (açık)                          | `surfaceContainerLowest` (`#FFFFFF`)          |
| Uygulama zemini (koyu)                              | `surface` (`#0C0E11`)                         |
| Kart / bottom sheet (koyu)                          | `surfaceContainerLow` (`#151517`)             |
| Başlık metni                                        | `onSurface`                                   |
| İkincil/açıklama metni (3 dk uzaklıkta vb.)         | `onSurfaceVariant`                            |
| Input / çip / pill zemini                           | `surfaceContainer`                            |
| Ayraç / kenarlık                                    | `outlineVariant`                              |
| "Kiralamayı Bitir", "Çıkış yap"                     | `error` / `onError`                           |
| "Müsait / Onaylı / Yüklendi" rozeti                 | `rencar.successContainer` + `rencar.success`  |
| Yakıt/şarj barı, +bakiye işlemi                     | `rencar.success`                              |
| Hasar uyarı satırı                                  | `rencar.warning` / `rencar.warningContainer`  |
| Harita fiyat balonu / kategori çipi                 | `rencar.catEconomy/Comfort/Suv/Electric/Busy` |

Örnek:

```kotlin
// "Kiralamayı Bitir" — tehlike butonu
Button(
    onClick = ::endRental,
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
    ),
) { Text("Kiralamayı Bitir") }

// "Müsait" rozeti
Surface(
    color = MaterialTheme.rencar.successContainer,
    contentColor = MaterialTheme.rencar.success,
    shape = RoundedCornerShape(8.dp),
) { Text("Müsait", Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }

// Konfor marker'ı
Icon(tint = MaterialTheme.rencar.catComfort, /* ... */)
```

---

## 7. Yap / Yapma

**Yap**

- Renklere daima `MaterialTheme.colorScheme.*` veya `MaterialTheme.rencar.*`
  üzerinden eriş.
- Metin renklerini `onX` çiftinden seç (ör. `surface` üstünde `onSurface`).
- Kategori renklerini yalnızca araç kimliği / harita için kullan.

**Yapma**

- `@Composable` içinde ham `Color(0xFF..)` kullanma — token'dan geç.
- Başarı için yeşili, tehlike için kırmızıyı başka anlamda kullanma.
- Koyu temada saf beyaz (`#FFFFFF`) büyük metin bloğu kullanma; `onSurface`
  (`#E4E2E6`) tercih et.

---

_Renk değerleri RenCar tasarım PDF'inden ve mavi tohumdan türetilmiştir.
Nihai marka mavisi (`#1A6BF0`) Figma/brand kaynağıyla birebir doğrulanmalıdır._