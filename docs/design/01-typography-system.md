# RenCar - Tipografi Sistemi

> Bu dosya RenCar isimli araç kiralama uygulamasının tipografi sistemi için
> **tek doğruluk kaynağıdır** (single source of truth). Doğrudan bir
> **Android Jetpack Compose** projesinde kullanılmak üzere düzenlenmiştir.

---

## Temel Kural

> Hiçbir `@Composable` içinde ham `TextStyle(fontSize = ...)` yazılmaz.
> Tipler daima `MaterialTheme.typography.<slot>` üzerinden okunur...

---

## Font Ailesi

> Tüm slotlarda sistem fontu **Roboto** (`FontFamily.Default`) kullanılır.
> Böylece proje ek font dosyası olmadan çalışır.
>
> Tasarımdaki geometrik/kalın görünümü birebir yakalamak istersen tek
> değişiklikle özel bir aileye geçebilirsin (bkz. "Özel Font — Opsiyonel").
> RenCar başlıkları **Bold**, büyük rakamlar (sayaç/fiyat) ise **ekstra kalın
> ve tabular** kullanılır (bkz. "Rakamlar / Tabular Figürler").

---

## `Type.kt`

```kotlin
package com.rencar.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val RencarTypography = Typography(
    displayLarge   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp),
    displayMedium  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.25).sp),
    displaySmall   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),

    headlineLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,     fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),

    titleLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    bodyLarge      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),

    labelLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)
```

> RenCar farkı: `display*` ve `headline*` slotları LyraApp'e göre daha kalın
> (Bold) ve başlıklarda negatif letter-spacing ile daha sıkı kuruldu — splash
> "Rencar", "Tekrar hoş geldin", "Kiralamalarım" gibi başlıkların iri/net
> duruşunu vermek için. Ölçüler (size/lineHeight) Material 3 baseline'dır.

---

## `Theme.kt` Entegrasyonu

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography  = RencarTypography, // ← Type.kt'den gelir
    content     = content,
)
```

---

## Slot → Tasarım Kullanımı

Tasarımdaki her metin hangi slotla yazılır:

| Slot             | RenCar'da kullanım                                               |
| ---------------- | ---------------------------------------------------------------- |
| `displayLarge`   | Splash "Rencar", aktif kiralama sayacı "00:24:18"                |
| `displayMedium`  | Cüzdan bakiyesi "₺340,00", büyük tutarlar                        |
| `displaySmall`   | Fiyat vurgusu "₺4,50 /dk"                                        |
| `headlineLarge`  | Ekran başlıkları: "Tekrar hoş geldin", "Kiralamalarım", "Cüzdan" |
| `headlineMedium` | "Telefonunu doğrula", "Yakınında 12 araç", "Yolculuk tamamlandı" |
| `headlineSmall`  | "Rezervasyon Onayı", "Araç durumu", kart üstü başlık             |
| `titleLarge`     | "Renault Clio" araç adı, bölüm başlıkları                        |
| `titleMedium`    | Liste satırı başlığı ("Ödeme yöntemleri"), profil adı            |
| `titleSmall`     | Kart alt başlığı, ikincil satır başlığı                          |
| `bodyLarge`      | Açıklama metni ("Telefon numaranı gir, SMS ile...")              |
| `bodyMedium`     | İkincil bilgi ("Kadıköy çevresinde · 3 dk uzaklıkta")            |
| `bodySmall`      | Alt bilgi / caption ("Son kullanma 08/27", tarih-saat)           |
| `labelLarge`     | Buton metni ("Hemen Başla", "Kod Gönder", "Kilidi Aç")           |
| `labelMedium`    | Sekme etiketleri (Harita/Geçmiş/Cüzdan/Profil), çip metni        |
| `labelSmall`     | Rozet/pill metni ("Müsait", "Onaylı", "24 dk", "12,4 km")        |

Örnek:

```kotlin
Text("Tekrar hoş geldin", style = MaterialTheme.typography.headlineLarge)

Text("Kadıköy çevresinde · 3 dk uzaklıkta",
     style = MaterialTheme.typography.bodyMedium,
     color = MaterialTheme.colorScheme.onSurfaceVariant)

Button(onClick = ::start) {
    Text("Hemen Başla", style = MaterialTheme.typography.labelLarge)
}
```

---

## Rakamlar / Tabular Figürler

Sayaç ("00:24:18") ve anlık ücret ("₺108,00") gibi **canlı güncellenen**
rakamlarda, basamaklar aynı genişlikte olsun (metin "zıplamasın") diye
tabular figürler kullan:

```kotlin
val TabularNumber = MaterialTheme.typography.displayLarge.copy(
    fontFeatureSettings = "tnum", // tabular (eşit genişlikli) rakamlar
)

Text("00:24:18", style = TabularNumber)
```

> Bu, ayrı bir slot değildir; ilgili `display*` slotunun `.copy(...)` ile
> türetilmiş halidir. Ham `TextStyle(fontSize = ...)` yazma kuralını bozmaz.

---

## Özel Font — Opsiyonel

Tasarımın geometrik görünümünü birebir istersen (ör. **Inter**), sadece bir
`FontFamily` tanımlayıp Type.kt'de `FontFamily.Default` yerine onu kullan.
Slot ölçüleri/ağırlıkları aynı kalır:

```kotlin
val Inter = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold),
)
// Type.kt'de her slotta: fontFamily = Inter
```

---

## Yap / Yapma

**Yap**

- Metin stillerine daima `MaterialTheme.typography.*` üzerinden eriş.
- Renk ile stili ayrı ver: `style = ...typography.bodyMedium`,
  `color = ...colorScheme.onSurfaceVariant`.
- Canlı rakamlarda tabular figür (`tnum`) kullan.

**Yapma**

- `@Composable` içinde ham `TextStyle(fontSize = 18.sp)` yazma; en yakın
  slotu seç, gerekiyorsa `.copy()` ile türet.
- Aynı hiyerarşi için farklı ekranlarda farklı slot kullanma (tutarlılık).
- Ağırlığı elle `Text(fontWeight = ...)` ile ezme; slot zaten doğru ağırlığı taşır.