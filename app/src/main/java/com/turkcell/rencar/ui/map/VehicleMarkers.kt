package com.turkcell.rencar.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.ceil

/**
 * Harita üzerindeki araçlar için "fiyat balonu" ikonlarını üretir. Harita stili (OSM_STYLE_JSON)
 * font/glyphs kaynağı içermediğinden SymbolLayer `text-field` kullanılamaz; bu yüzden araç
 * ikonu + fiyat yazısı Canvas ile bir bitmap'e çizilip [org.maplibre.android.maps.Style.addImage]
 * ile ikon olarak eklenir. Renk, aracın API tipine göre seçilir (marka kimliği, tema bağımsız).
 */
object VehicleMarkers {

    // Araç tipi → balon rengi (ARGB). Değerler Color.kt'deki tema bağımsız Cat* tonlarıyla aynı;
    // Compose Color'a bağımlı kalmamak için android.graphics tarafında hex olarak tutulur.
    private const val BRAND_BLUE = 0xFF1A6BF0.toInt() // LightPrimary (SEDAN + varsayılan)
    private const val CAT_SUV = 0xFFF5B301.toInt()    // CatSuv
    private const val CAT_ECONOMY = 0xFFF97316.toInt() // CatEconomy
    private const val CAT_COMFORT = 0xFF7C4DFF.toInt() // CatComfort
    private const val CAT_ELECTRIC = 0xFF14B8A6.toInt() // CatElectric

    /** API araç tipini balon rengine eşler (SEDAN/SUV/HATCHBACK/STATION/MINIVAN). */
    fun colorForType(type: String): Int = when (type.uppercase()) {
        "SUV" -> CAT_SUV
        "HATCHBACK" -> CAT_ECONOMY
        "STATION" -> CAT_COMFORT
        "MINIVAN" -> CAT_ELECTRIC
        "SEDAN" -> BRAND_BLUE
        else -> BRAND_BLUE
    }

    /** Günlük fiyatı "₺1500" biçiminde etikete dönüştürür (kuruş yuvarlanır). */
    fun priceLabel(pricePerDay: Double): String = "₺" + pricePerDay.toLong()

    /**
     * Verilen [priceText] ve [backgroundColor] için, alt ucu konuma bakan (bottom-anchor)
     * yuvarlak fiyat balonu bitmap'i üretir. Ölçüler cihaz yoğunluğuna (dp) göre ölçeklenir.
     */
    fun build(context: Context, priceText: String, backgroundColor: Int): Bitmap {
        val d = context.resources.displayMetrics.density
        fun dp(v: Float) = v * d

        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(13f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }

        val iconSize = dp(16f)
        val padH = dp(10f)
        val gap = dp(5f)
        val pillH = dp(30f)
        val tailH = dp(6f)
        val tailW = dp(10f)
        val margin = dp(4f) // gölge + tail taşması için kenar boşluğu

        val textWidth = textPaint.measureText(priceText)
        val pillW = padH + iconSize + gap + textWidth + padH
        val width = ceil(pillW + margin * 2).toInt()
        val height = ceil(pillH + tailH + margin * 2).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val left = margin
        val top = margin
        val right = left + pillW
        val bottom = top + pillH
        val centerX = (left + right) / 2f

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            setShadowLayer(dp(3f), 0f, dp(1f), 0x40000000)
        }

        // Balon gövdesi (tam yuvarlatılmış = kapsül).
        val radius = pillH / 2f
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, bgPaint)

        // Alt ok (tail) — konum noktasını işaret eder.
        val tail = Path().apply {
            moveTo(centerX - tailW / 2f, bottom - dp(1f))
            lineTo(centerX + tailW / 2f, bottom - dp(1f))
            lineTo(centerX, bottom + tailH)
            close()
        }
        canvas.drawPath(tail, bgPaint)

        // Araç silueti (beyaz) — sol tarafta.
        drawCar(canvas, left + padH, top + (pillH - iconSize) / 2f, iconSize, white)

        // Fiyat yazısı — dikeyde ortalanmış.
        val fm = textPaint.fontMetrics
        val baseline = top + pillH / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(priceText, left + padH + iconSize + gap, baseline, textPaint)

        return bitmap
    }

    /** [size] boyutlu kutuya basit bir beyaz araç silueti (kabin + gövde + tekerler) çizer. */
    private fun drawCar(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val cabin = Path().apply {
            moveTo(x + size * 0.26f, y + size * 0.46f)
            lineTo(x + size * 0.38f, y + size * 0.24f)
            lineTo(x + size * 0.66f, y + size * 0.24f)
            lineTo(x + size * 0.78f, y + size * 0.46f)
            close()
        }
        canvas.drawPath(cabin, paint)
        canvas.drawRoundRect(
            RectF(x, y + size * 0.44f, x + size, y + size * 0.70f),
            size * 0.13f, size * 0.13f, paint,
        )
        canvas.drawCircle(x + size * 0.27f, y + size * 0.72f, size * 0.11f, paint)
        canvas.drawCircle(x + size * 0.73f, y + size * 0.72f, size * 0.11f, paint)
    }
}
