package com.turkcell.rencar.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.ceil

/**
 * Harita üzerindeki araçlar için "fiyat balonu" ikonlarını üretir. Harita stili font/glyphs
 * kaynağı içermediğinden SymbolLayer `text-field` kullanılamaz; bu yüzden araç ikonu + yazı
 * Canvas ile bir bitmap'e çizilip [org.maplibre.android.maps.Style.addImage] ile ikon olarak
 * eklenir. Renk, aracın segmentine (yoksa tipine) göre seçilir; kullanımdaki (busy) araçlar
 * gri çizilir. Balonun etrafına, tasarımdaki gibi kategori renginde yumuşak bir "glow" eklenir.
 */
object VehicleMarkers {

    // Kategori → balon rengi (ARGB). Color.kt'deki tema bağımsız Cat* tonlarıyla birebir;
    // Compose Color'a bağımlı kalmamak için android.graphics tarafında hex olarak tutulur.
    private const val BRAND_BLUE = 0xFF1A6BF0.toInt() // LightPrimary (SEDAN + varsayılan)
    private const val CAT_SUV = 0xFFF5B301.toInt()     // CatSuv (🟡)
    private const val CAT_ECONOMY = 0xFFF97316.toInt()  // CatEconomy (🟠)
    private const val CAT_COMFORT = 0xFF7C4DFF.toInt()  // CatComfort (🟣)
    private const val CAT_ELECTRIC = 0xFF14B8A6.toInt() // CatElectric (🟢)
    private const val CAT_BUSY = 0xFF64748B.toInt()     // CatBusy (⚪ Kullanımda)

    /** Araç AVAILABLE mı (marker rengi/etiketi/tıklanabilirliği buna bağlıdır). */
    fun isAvailable(status: String): Boolean = status.uppercase() == "AVAILABLE"

    /**
     * Balon rengini seçer: kullanımdaki araç → gri; müsait araç → segment rengi
     * (ECONOMY/COMFORT/SUV), segment yoksa tip rengine düşer.
     */
    fun colorFor(segment: String?, type: String, status: String): Int {
        if (!isAvailable(status)) return CAT_BUSY
        return when (segment?.uppercase()) {
            "ECONOMY" -> CAT_ECONOMY
            "COMFORT" -> CAT_COMFORT
            "SUV" -> CAT_SUV
            else -> colorForType(type)
        }
    }

    /** API araç tipini balon rengine eşler (segment yokken yedek). */
    private fun colorForType(type: String): Int = when (type.uppercase()) {
        "SUV" -> CAT_SUV
        "HATCHBACK" -> CAT_ECONOMY
        "STATION" -> CAT_COMFORT
        "MINIVAN" -> CAT_ELECTRIC
        "SEDAN" -> BRAND_BLUE
        else -> BRAND_BLUE
    }

    /** Balon etiketi: müsaitse günlük fiyat ("₺1500"), kullanımdaysa "Kullanımda". */
    fun labelFor(status: String, pricePerDay: Double): String =
        if (isAvailable(status)) priceLabel(pricePerDay) else "Kullanımda"

    /** Günlük fiyatı "₺1500" biçiminde etikete dönüştürür (kuruş yuvarlanır). */
    private fun priceLabel(pricePerDay: Double): String = "₺" + pricePerDay.toLong()

    /**
     * Verilen [label] ve [backgroundColor] için, alt ucu konuma bakan (bottom-anchor) yuvarlak
     * balon bitmap'i üretir; etrafına kategori renginde yumuşak glow çizer. [glow] false ise
     * (ör. kullanımdaki araç) halo daha sönük kalır. Ölçüler cihaz yoğunluğuna (dp) göre ölçeklenir.
     */
    fun build(context: Context, label: String, backgroundColor: Int, glow: Boolean = true): Bitmap {
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
        val glowRadius = dp(9f)
        val margin = dp(12f) // glow + gölge + tail taşması için kenar boşluğu

        val textWidth = textPaint.measureText(label)
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
        val radius = pillH / 2f

        // Glow — balonun arkasına kategori renginde bulanık halo (tasarımdaki neon parıltı).
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            alpha = if (glow) 150 else 60
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, glowPaint)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = backgroundColor
            setShadowLayer(dp(3f), 0f, dp(1f), 0x40000000)
        }

        // Balon gövdesi (tam yuvarlatılmış = kapsül).
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

        // Etiket yazısı — dikeyde ortalanmış.
        val fm = textPaint.fontMetrics
        val baseline = top + pillH / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText(label, left + padH + iconSize + gap, baseline, textPaint)

        return bitmap
    }

    /**
     * Aktif yolculuk haritasındaki araç için merkez-çapalı (center-anchor) yuvarlak pin üretir:
     * marka mavisi dolgu + beyaz halka + ortada beyaz araç silüeti, arkasında yumuşak glow.
     * Tasarımdaki (Aktif Yolculuk) tekil araç işaretçisiyle birebir; fiyat balonundan farklıdır.
     */
    fun buildRidePin(context: Context): Bitmap {
        val d = context.resources.displayMetrics.density
        fun dp(v: Float) = v * d

        val diameter = dp(42f)
        val ringWidth = dp(3f)
        val glowRadius = dp(10f)
        val margin = glowRadius + dp(4f)
        val size = ceil(diameter + margin * 2).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = diameter / 2f

        // Glow — marka mavisi bulanık halo.
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_BLUE
            alpha = 150
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(cx, cy, radius, glowPaint)

        // Beyaz halka (kenarlık) + hafif gölge.
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(dp(3f), 0f, dp(1f), 0x40000000)
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Mavi dolgu.
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BRAND_BLUE }
        canvas.drawCircle(cx, cy, radius - ringWidth, fillPaint)

        // Ortada beyaz araç silüeti.
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val iconSize = dp(22f)
        drawCar(canvas, cx - iconSize / 2f, cy - iconSize / 2f, iconSize, white)

        return bitmap
    }

    /**
     * Küme (cluster) "sayı balonu": marka mavisi dolgu + beyaz halka + ortada beyaz kalın sayı,
     * arkasında yumuşak glow. [label] küme büyüklüğüdür (ör. "12"); balon çapı metne göre büyür.
     * Stilde glyph/font olmadığından sayı Canvas ile bitmap'e çizilir (fiyat balonlarıyla aynı yaklaşım).
     */
    fun buildCluster(context: Context, label: String): Bitmap {
        val d = context.resources.displayMetrics.density
        fun dp(v: Float) = v * d

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = dp(15f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val ringWidth = dp(3f)
        val glowRadius = dp(9f)
        val margin = glowRadius + dp(4f)
        // Çap metinle büyür (çok haneli kümeler taşmasın); alt sınır sabit.
        val diameter = maxOf(dp(40f), textPaint.measureText(label) + dp(26f))
        val size = ceil(diameter + margin * 2).toInt()

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radius = diameter / 2f

        // Glow — marka mavisi bulanık halo.
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BRAND_BLUE
            alpha = 150
            maskFilter = BlurMaskFilter(glowRadius, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(cx, cy, radius, glowPaint)

        // Beyaz halka + hafif gölge.
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(dp(3f), 0f, dp(1f), 0x40000000)
        }
        canvas.drawCircle(cx, cy, radius, ringPaint)

        // Mavi dolgu.
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BRAND_BLUE }
        canvas.drawCircle(cx, cy, radius - ringWidth, fillPaint)

        // Sayı — dikeyde ortalanmış.
        val fm = textPaint.fontMetrics
        canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2f, textPaint)

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
