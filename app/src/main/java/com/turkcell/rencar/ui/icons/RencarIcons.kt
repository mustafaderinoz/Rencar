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
                // Gövde
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
                // Sol teker
                moveTo(6.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveTo(5.67f, 13f, 6.5f, 13f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveTo(7.33f, 16f, 6.5f, 16f)
                close()
                // Sağ teker
                moveTo(17.5f, 16f)
                curveToRelative(-0.83f, 0f, -1.5f, -0.67f, -1.5f, -1.5f)
                reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
                reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
                reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
                close()
                // Kabin / ön cam
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
}
