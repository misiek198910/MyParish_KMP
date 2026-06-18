package com.example.mojaparafia.ui.components

import android.content.Context
import android.graphics.*
import android.util.LruCache
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.mojaparafia.R


class ParishRenderer(
    private val mContext: Context,
    map: GoogleMap?,
    clusterManager: ClusterManager<ParishClusterItem>?
) : DefaultClusterRenderer<ParishClusterItem>(mContext, map, clusterManager) {

    private val iconCache = LruCache<String, BitmapDescriptor>(150)

    // Osobny cache dla klastrów (żeby nie wywalać pojedynczych ikon)
    private val clusterIconCache = LruCache<String, BitmapDescriptor>(150)

    // Jedyne źródło prawdy o motywie - ustawiane z MainActivity
    var isNightMode: Boolean = false

    // Wymuszamy klastrowanie już od 2 parafii (lepszy efekt "mapy ciepła")
    override fun shouldRenderAsCluster(cluster: Cluster<ParishClusterItem>): Boolean {
        return cluster.size > 1
    }

    // =========================================================================
    // 1. RENDEROWANIE POJEDYNCZEJ PARAFII
    // =========================================================================
    override fun onBeforeClusterItemRendered(item: ParishClusterItem, markerOptions: MarkerOptions) {
        val iconResId = if (item.isCathedral == 1) R.drawable.ic_cathedral else R.drawable.ic_church
        val hasCrown = item.isHomeParish && item.userHasCrown
        val darkModeActive = isNightMode

        val cacheKey = "${iconResId}_f${item.isFavorite}_i${item.activeIntentions}_c${item.activeCandles}_h${hasCrown}_n$darkModeActive"

        var descriptor = iconCache.get(cacheKey)
        if (descriptor == null) {
            descriptor = bitmapDescriptorFromVector(
                resId = iconResId,
                isFavorite = item.isFavorite,
                intentions = item.activeIntentions,
                candles = item.activeCandles,
                hasCrown = hasCrown,
                isDarkMode = darkModeActive
            )
            descriptor?.let { iconCache.put(cacheKey, it) }
        }

        descriptor?.let {
            markerOptions.icon(it)
            markerOptions.anchor(0.5f, 0.5f)
        }

        val basePriority = if (item.activeCandles > 0) 20f else 1f
        markerOptions.zIndex(basePriority + (if (hasCrown) 5f else 0f) + (if (item.isFavorite) 2f else 0f))
    }

    // =========================================================================
    // 2. RENDEROWANIE KLASTRA (REGIONU)
    // =========================================================================
    override fun onBeforeClusterRendered(cluster: Cluster<ParishClusterItem>, markerOptions: MarkerOptions) {
        var totalIntentions = 0
        var totalCandles = 0

        // Zliczamy cały "ogień" i intencje wewnątrz tego klastra
        for (item in cluster.items) {
            totalIntentions += item.activeIntentions
            totalCandles += item.activeCandles
        }

        val darkModeActive = isNightMode
        val cacheKey = "cluster_${cluster.size}_i${totalIntentions}_c${totalCandles}_n$darkModeActive"

        var descriptor = clusterIconCache.get(cacheKey)
        if (descriptor == null) {
            descriptor = bitmapDescriptorForCluster(
                clusterSize = cluster.size,
                totalIntentions = totalIntentions,
                totalCandles = totalCandles,
                isDarkMode = darkModeActive
            )
            descriptor?.let { clusterIconCache.put(cacheKey, it) }
        }

        descriptor?.let {
            markerOptions.icon(it)
            // Dzięki Symetrycznemu Canvasowi klaster też jest idealnie na środku
            markerOptions.anchor(0.5f, 0.5f)
        }

        // Świecące klastry regionalne mają absolutny priorytet rysowania nad wszystkim
        val basePriority = if (totalCandles > 0) 30f else 5f
        markerOptions.zIndex(basePriority)
    }

    // =========================================================================
    // GENERATOR KLASTRA (NOWOŚĆ)
    // =========================================================================
    private fun bitmapDescriptorForCluster(
        clusterSize: Int,
        totalIntentions: Int,
        totalCandles: Int,
        isDarkMode: Boolean
    ): BitmapDescriptor? {
        val density = mContext.resources.displayMetrics.density

        // Rozmiar okręgu klastra zależy od ilości cyfr (żeby np. "150" nie wychodziło za okrąg)
        val textLength = clusterSize.toString().length
        val circleRadius = (18 * density + (textLength * 2 * density)).coerceAtMost(35 * density)

        // POŚWIATA REGIONALNA: Mnożnik jest mniejszy (bo świec mogą być setki),
        // ale limit (coerceAtMost) jest ogromny - aż 200dp promienia (400dp średnicy).
        val currentGlowRadius = if (isDarkMode && totalCandles > 0) {
            (circleRadius + (totalCandles * 5 * density)).coerceAtMost(200 * density)
        } else 0f

        // MATEMATYKA SYMETRYCZNEGO CANVASU
        val topPaddingNeeded = 35 * density // Miejsce na pastylkę sumującą nad klastrem
        val bottomPaddingNeeded = currentGlowRadius
        val sidePaddingNeeded = maxOf(currentGlowRadius, 60 * density)

        val maxPadding = maxOf(topPaddingNeeded, bottomPaddingNeeded, sidePaddingNeeded)
        val canvasSize = ((circleRadius * 2) + (maxPadding * 2)).toInt()

        val bitmap = createBitmap(canvasSize, canvasSize)
        val canvas = Canvas(bitmap)

        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        // 1. RYSOWANIE OGROMNEJ POŚWIATY (Tylko w trybie nocnym)
        if (currentGlowRadius > 0) {
            // Szybko osiągamy maksymalny blask środka przy dużych klastrach
            val midColorAlpha = (100 + (totalCandles * 10)).coerceAtMost(255)
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX, centerY, currentGlowRadius,
                    intArrayOf(
                        Color.argb(255, 255, 100, 0),             // Ciemniejszy, głęboki rdzeń ognia
                        Color.argb(midColorAlpha, 255, 193, 7),
                        Color.argb(0, 255, 193, 7)                // Przezroczysty żółty
                    ),
                    floatArrayOf(0f, 0.4f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerX, centerY, currentGlowRadius, glowPaint)
        }

        // 2. RYSOWANIE KOŁA KLASTRA
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            // Jeśli region płonie, klaster robi się pomarańczowy. Jeśli nie, zostaje domyślny niebieski.
            color = if (totalCandles > 0) "#E65100".toColorInt() else "#1976D2".toColorInt()
            setShadowLayer(4f, 0f, 2f, Color.argb(120, 0,0,0))
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
        }

        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
        canvas.drawCircle(centerX, centerY, circleRadius, strokePaint)

        // 3. TEKST W ŚRODKU (Liczba parafii wewnątrz klastra)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 14 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textBounds = Rect()
        val label = clusterSize.toString()
        textPaint.getTextBounds(label, 0, label.length, textBounds)
        val textY = centerY + textBounds.height() / 2f
        canvas.drawText(label, centerX, textY, textPaint)

        // 4. PASTYLKA Z SUMĄ INTENCJI I ŚWIEC (Nad klastrem)
        if (totalIntentions > 0 || totalCandles > 0) {
            val topElementY = centerY - circleRadius
            drawExpandedBadge(canvas, centerX, topElementY - (4 * density), totalIntentions, totalCandles, density, isDarkMode)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // =========================================================================
    // GENERATOR POJEDYNCZEGO KOŚCIOŁA (ZACHOWANY)
    // =========================================================================
    private fun bitmapDescriptorFromVector(
        resId: Int, isFavorite: Boolean, intentions: Int, candles: Int, hasCrown: Boolean, isDarkMode: Boolean
    ): BitmapDescriptor? {
        val density = mContext.resources.displayMetrics.density
        val churchSize = (40 * density).toInt()
        val crownSize = (24 * density).toInt()
        val starSize = (16 * density).toInt()

        val currentGlowRadius = if (isDarkMode && candles > 0) {
            (churchSize * 0.8f + (candles * 15 * density)).coerceAtMost(180 * density)
        } else 0f

        val topPaddingNeeded = (if (hasCrown) crownSize else 0) + (35 * density)
        val bottomPaddingNeeded = currentGlowRadius
        val sidePaddingNeeded = maxOf(currentGlowRadius, 60 * density)

        val maxPadding = maxOf(topPaddingNeeded, bottomPaddingNeeded, sidePaddingNeeded)
        val canvasSize = (churchSize + (maxPadding * 2)).toInt()
        val bitmap = createBitmap(canvasSize, canvasSize)
        val canvas = Canvas(bitmap)

        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        if (currentGlowRadius > 0) {
            val midColorAlpha = (100 + (candles * 30)).coerceAtMost(255)
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    centerX, centerY, currentGlowRadius,
                    intArrayOf(
                        Color.argb(255, 255, 120, 0),
                        Color.argb(midColorAlpha, 255, 193, 7),
                        Color.argb(0, 255, 193, 7)
                    ),
                    floatArrayOf(0f, 0.3f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerX, centerY, currentGlowRadius, glowPaint)
        }

        val churchDrawable = ContextCompat.getDrawable(mContext, resId) ?: return null
        val cL = (centerX - churchSize / 2).toInt()
        val cT = (centerY - churchSize / 2).toInt()
        churchDrawable.setBounds(cL, cT, cL + churchSize, cT + churchSize)
        churchDrawable.draw(canvas)

        var topElementY = centerY - churchSize / 2f
        if (hasCrown) {
            val crownDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_crown)
            crownDrawable?.let {
                it.colorFilter = PorterDuffColorFilter("#FFD700".toColorInt(), PorterDuff.Mode.SRC_IN)
                val crL = (centerX - crownSize / 2).toInt()
                val crT = (centerY - churchSize / 1.8f - crownSize).toInt()
                it.setBounds(crL, crT, crL + crownSize, crT + crownSize)
                it.draw(canvas)
                topElementY = crT.toFloat()
            }
        }

        if (isFavorite) {
            drawStar(canvas, centerX + churchSize/3, centerY - churchSize/3, starSize/2f, density)
        }

        if (intentions > 0 || candles > 0) {
            drawExpandedBadge(canvas, centerX, topElementY - (4 * density), intentions, candles, density, isDarkMode)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, density: Float) {
        val path = Path()
        val innerRadius = radius * 0.45f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = Math.toRadians(i * 36.0 - 90.0)
            val x = (cx + r * Math.cos(angle)).toFloat()
            val y = (cy + r * Math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = "#FFD700".toColorInt() })
        canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; style = Paint.Style.STROKE; strokeWidth = 1 * density
        })
    }

    private fun drawExpandedBadge(canvas: Canvas, cx: Float, bottomY: Float, intentions: Int, candles: Int, density: Float, isDarkMode: Boolean) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 12 * density
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val label = "🙏 $intentions    🕯️ $candles"
        val textBounds = Rect()
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val padH = 10 * density
        val padV = 5 * density
        val w = textBounds.width() + padH * 2
        val h = textBounds.height() + padV * 2

        val rect = RectF(cx - w / 2, bottomY - h, cx + w / 2, bottomY)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDarkMode && candles > 0) {
                "#E65100".toColorInt()
            } else {
                "#1A252F".toColorInt()
            }
            setShadowLayer(3f, 0f, 2f, Color.argb(100, 0,0,0))
        }

        canvas.drawRoundRect(rect, 10 * density, 10 * density, bgPaint)
        val textY = rect.centerY() + textBounds.height() / 2f - (1.5f * density)
        canvas.drawText(label, cx, textY, textPaint)
    }
}