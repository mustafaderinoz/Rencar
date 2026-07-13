package com.turkcell.rencar.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Uygulama genelinde kullanılan özel (bağımlılıksız) vektör ikonları.
 * Tüm ikonlar bu dosyada, [ImageVector] olarak tutulur; renk çağrı yerinde
 * `Icon(tint = ...)` ile verilir.
 */
object RencarIcons {

    /** Marka logosu içindeki araç silueti (24dp viewport). */
    val Car: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarCar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(18.92f, 6.01f)
                curveTo(18.72f, 5.42f, 18.16f, 5f, 17.5f, 5f)
                horizontalLineToRelative(-11f)
                curveToRelative(-0.66f, 0f, -1.21f, 0.42f, -1.42f, 1.01f)
                lineTo(3f, 12f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-1f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 0.55f, 0.45f, 1f, 1f, 1f)
                horizontalLineToRelative(1f)
                curveToRelative(0.55f, 0f, 1f, -0.45f, 1f, -1f)
                verticalLineToRelative(-8f)
                lineToRelative(-2.08f, -5.99f)
                close()
                moveTo(6.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(5.67f, 13f, 6.5f, 13f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(7.33f, 16f, 6.5f, 16f)
                close()
                moveTo(17.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
                moveTo(5f, 11f)
                lineToRelative(1.5f, -4.5f)
                horizontalLineToRelative(11f)
                lineTo(19f, 11f)
                close()
            }
        }.build()
    }

    /** Geri (‹) — üst-sol geri butonu. Tint çağrı yerinde verilir. */
    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(15.41f, 7.41f)
                lineTo(14f, 6f)
                lineTo(8f, 12f)
                lineTo(14f, 18f)
                lineTo(15.41f, 16.59f)
                lineTo(10.83f, 12f)
                close()
            }
        }.build()
    }

    /** Sohbet balonu — "Kod Gönder" butonu ikonu. */
    val ChatBubble: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarChatBubble",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(2f, 22f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
            }
        }.build()
    }

    /** Bilgi (ⓘ) — telefon alanı altındaki açıklama satırı. */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarInfo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveToRelative(1f, 15f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                close()
                moveToRelative(0f, -8f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
            }
        }.build()
    }

    /** Telefon/cihaz silueti — OTP ekranı üst ikonu. */
    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarPhone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 1f)
                horizontalLineTo(7f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(18f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(10f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(3f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveToRelative(0f, 18f)
                horizontalLineTo(7f)
                verticalLineTo(5f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(14f)
                close()
            }
        }.build()
    }

    /** Saat (yeniden gönderme sayacı) — OTP ekranı zamanlayıcı satırı. */
    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                // Dış çember
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveToRelative(0f, 18f)
                curveToRelative(-4.41f, 0f, -8f, -3.59f, -8f, -8f)
                reflectiveCurveToRelative(3.59f, -8f, 8f, -8f)
                reflectiveCurveToRelative(8f, 3.59f, 8f, 8f)
                reflectiveCurveToRelative(-3.59f, 8f, -8f, 8f)
                close()
                // Akrep + yelkovan
                moveTo(12.5f, 7f)
                horizontalLineToRelative(-1.5f)
                verticalLineToRelative(6f)
                lineToRelative(5.25f, 3.15f)
                lineToRelative(0.75f, -1.23f)
                lineToRelative(-4.5f, -2.67f)
                close()
            }
        }.build()
    }

    /** Konum iğnesi — alt navigasyon "Harita" sekmesi. */
    val MapPin: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarMapPin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(8.13f, 2f, 5f, 5.13f, 5f, 9f)
                curveToRelative(0f, 5.25f, 7f, 13f, 7f, 13f)
                reflectiveCurveToRelative(7f, -7.75f, 7f, -13f)
                curveToRelative(0f, -3.87f, -3.13f, -7f, -7f, -7f)
                close()
                moveToRelative(0f, 9.5f)
                curveToRelative(-1.38f, 0f, -2.5f, -1.12f, -2.5f, -2.5f)
                reflectiveCurveToRelative(1.12f, -2.5f, 2.5f, -2.5f)
                reflectiveCurveToRelative(2.5f, 1.12f, 2.5f, 2.5f)
                reflectiveCurveToRelative(-1.12f, 2.5f, -2.5f, 2.5f)
                close()
            }
        }.build()
    }

    /** Nişangâh (crosshair) — harita "konumuma git" FAB'ı. */
    val MyLocation: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarMyLocation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                curveToRelative(-2.21f, 0f, -4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                reflectiveCurveToRelative(4f, -1.79f, 4f, -4f)
                reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
                close()
                moveTo(20.94f, 11f)
                curveToRelative(-0.46f, -4.17f, -3.77f, -7.48f, -7.94f, -7.94f)
                verticalLineTo(1f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(2.06f)
                curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
                horizontalLineTo(1f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(2.06f)
                curveToRelative(0.46f, 4.17f, 3.77f, 7.48f, 7.94f, 7.94f)
                verticalLineTo(23f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(-2.06f)
                curveToRelative(4.17f, -0.46f, 7.48f, -3.77f, 7.94f, -7.94f)
                horizontalLineTo(23f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(-2.06f)
                close()
                moveTo(12f, 19f)
                curveToRelative(-3.87f, 0f, -7f, -3.13f, -7f, -7f)
                reflectiveCurveToRelative(3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                close()
            }
        }.build()
    }

    /** Geri saat oku (geçmiş) — alt navigasyon "Geçmiş" sekmesi. */
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarHistory",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 3f)
                curveToRelative(-4.97f, 0f, -9f, 4.03f, -9f, 9f)
                horizontalLineTo(1f)
                lineToRelative(3.89f, 3.89f)
                lineToRelative(0.07f, 0.14f)
                lineTo(9f, 12f)
                horizontalLineTo(6f)
                curveToRelative(0f, -3.87f, 3.13f, -7f, 7f, -7f)
                reflectiveCurveToRelative(7f, 3.13f, 7f, 7f)
                reflectiveCurveToRelative(-3.13f, 7f, -7f, 7f)
                curveToRelative(-1.93f, 0f, -3.68f, -0.79f, -4.94f, -2.06f)
                lineToRelative(-1.42f, 1.42f)
                curveTo(8.27f, 19.99f, 10.51f, 21f, 13f, 21f)
                curveToRelative(4.97f, 0f, 9f, -4.03f, 9f, -9f)
                reflectiveCurveToRelative(-4.03f, -9f, -9f, -9f)
                close()
                moveToRelative(-1f, 5f)
                verticalLineToRelative(5f)
                lineToRelative(4.28f, 2.54f)
                lineToRelative(0.72f, -1.21f)
                lineToRelative(-3.5f, -2.08f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                close()
            }
        }.build()
    }

    /** Cüzdan — alt navigasyon "Cüzdan" sekmesi. */
    val Wallet: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarWallet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(21f, 18f)
                verticalLineToRelative(1f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -2f, -0.9f, -2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, 0.89f, -2f, 2f, -2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(-9f)
                curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(8f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(9f)
                close()
                moveToRelative(-9f, -2f)
                horizontalLineToRelative(10f)
                verticalLineTo(8f)
                horizontalLineTo(12f)
                verticalLineToRelative(8f)
                close()
                moveToRelative(4f, -2.5f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
            }
        }.build()
    }

    /** Fotoğraf makinesi — ehliyet "çek veya yükle" kartları. */
    val Camera: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarCamera",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 2f)
                lineTo(7.17f, 4f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(16f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(6f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-3.17f)
                lineTo(15f, 2f)
                horizontalLineTo(9f)
                close()
                moveTo(12f, 17f)
                curveToRelative(-2.76f, 0f, -5f, -2.24f, -5f, -5f)
                reflectiveCurveToRelative(2.24f, -5f, 5f, -5f)
                reflectiveCurveToRelative(5f, 2.24f, 5f, 5f)
                reflectiveCurveToRelative(-2.24f, 5f, -5f, 5f)
                close()
                moveTo(12f, 15.2f)
                curveToRelative(1.77f, 0f, 3.2f, -1.43f, 3.2f, -3.2f)
                reflectiveCurveToRelative(-1.43f, -3.2f, -3.2f, -3.2f)
                reflectiveCurveToRelative(-3.2f, 1.43f, -3.2f, 3.2f)
                reflectiveCurveToRelative(1.43f, 3.2f, 3.2f, 3.2f)
                close()
            }
        }.build()
    }

    /** Onay tiki — "Yüklendi" rozeti ve başarı durumu. */
    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(9f, 16.17f)
                lineTo(4.83f, 12f)
                lineToRelative(-1.42f, 1.41f)
                lineTo(9f, 19f)
                lineTo(21f, 7f)
                lineToRelative(-1.41f, -1.41f)
                close()
            }
        }.build()
    }

    /** İleri (›) — devam/ilerleme yön ikonu. */
    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 6f)
                lineTo(8.59f, 7.41f)
                lineTo(13.17f, 12f)
                lineToRelative(-4.58f, 4.59f)
                lineTo(10f, 18f)
                lineToRelative(6f, -6f)
                close()
            }
        }.build()
    }

    /** Yakıt pompası — araç detay "Yakıt" kartı. */
    val Fuel: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarFuel",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.77f, 7.23f)
                lineToRelative(0.01f, -0.01f)
                lineToRelative(-3.72f, -3.72f)
                lineTo(15f, 4.56f)
                lineToRelative(2.11f, 2.11f)
                curveToRelative(-0.94f, 0.36f, -1.61f, 1.26f, -1.61f, 2.33f)
                curveToRelative(0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
                curveToRelative(0.36f, 0f, 0.69f, -0.08f, 1f, -0.21f)
                verticalLineToRelative(7.21f)
                curveToRelative(0f, 0.55f, -0.45f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.45f, -1f, -1f)
                verticalLineTo(14f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineToRelative(-1f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                horizontalLineTo(6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(16f)
                horizontalLineToRelative(10f)
                verticalLineToRelative(-7.5f)
                horizontalLineToRelative(1.5f)
                verticalLineToRelative(5f)
                curveToRelative(0f, 1.38f, 1.12f, 2.5f, 2.5f, 2.5f)
                reflectiveCurveToRelative(2.5f, -1.12f, 2.5f, -2.5f)
                verticalLineTo(9f)
                curveToRelative(0f, -0.69f, -0.28f, -1.32f, -0.73f, -1.77f)
                close()
                moveTo(12f, 10f)
                horizontalLineTo(6f)
                verticalLineTo(5f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(5f)
                close()
            }
        }.build()
    }

    /** Dişli/çark — araç detay "Vites" kartı (mekanik). */
    val Gear: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarGear",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineToRelative(-0.36f, -2.54f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineToRelative(-0.36f, 2.54f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
                reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineToRelative(-2.01f, -1.58f)
                close()
                moveTo(12f, 15.6f)
                curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
                close()
            }
        }.build()
    }

    /** Koltuk silueti (L biçimi: sırtlık + oturak) — araç detay "Koltuk" kartı. */
    val Seat: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarSeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 4f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(11f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(3f)
                horizontalLineTo(7f)
                close()
            }
        }.build()
    }

    /** Kilit (asma kilit) — araç detay "Kilidi Aç" butonu. */
    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarLock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(18f, 8f)
                horizontalLineToRelative(-1f)
                verticalLineTo(6f)
                curveToRelative(0f, -2.76f, -2.24f, -5f, -5f, -5f)
                reflectiveCurveTo(7f, 3.24f, 7f, 6f)
                verticalLineToRelative(2f)
                horizontalLineTo(6f)
                curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(10f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(10f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(8.9f, 6f)
                curveToRelative(0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
                reflectiveCurveToRelative(3.1f, 1.39f, 3.1f, 3.1f)
                verticalLineToRelative(2f)
                horizontalLineTo(8.9f)
                verticalLineTo(6f)
                close()
            }
        }.build()
    }

    /** Kişi silueti — alt navigasyon "Profil" sekmesi. */
    val Person: ImageVector by lazy {
        ImageVector.Builder(
            name = "RencarPerson",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
                reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
                reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                close()
                moveToRelative(0f, 2f)
                curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-2f)
                curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
                close()
            }
        }.build()
    }
}