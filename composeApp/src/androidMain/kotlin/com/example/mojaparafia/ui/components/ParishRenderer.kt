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
    private val clusterIconCache = LruCache<String, BitmapDescriptor>(150)

    // Jedyne źródło prawdy o motywie - ustawiane z kontrolera mapy
    var isNightMode: Boolean = false

    override fun shouldRenderAsCluster(cluster: Cluster<ParishClusterItem>): Boolean {
        return cluster.size > 1
    }

    override fun onBeforeClusterItemRendered(item: ParishClusterItem, markerOptions: MarkerOptions) {
        val iconResId = if (item.isCathedral == 1) R.drawable.ic_cathedral else R.drawable.ic_church

        // Uwzględniamy tryb nocny w kluczu pamięci podręcznej (cacheKey)
        val cacheKey = "${iconResId}_f${item.isFavorite}_n${isNightMode}"

        var descriptor = iconCache.get(cacheKey)
        if (descriptor == null) {
            descriptor = bitmapDescriptorFromVector(
                resId = iconResId,
                isFavorite = item.isFavorite
            )
            descriptor?.let { iconCache.put(cacheKey, it) }
        }

        descriptor?.let {
            markerOptions.icon(it)
            markerOptions.anchor(0.5f, 0.5f)
        }

        markerOptions.zIndex(1f + (if (item.isFavorite) 2f else 0f))
    }

    override fun onBeforeClusterRendered(cluster: Cluster<ParishClusterItem>, markerOptions: MarkerOptions) {
        // Uwzględniamy tryb nocny w kluczu pamięci podręcznej
        val cacheKey = "cluster_${cluster.size}_n${isNightMode}"

        var descriptor = clusterIconCache.get(cacheKey)
        if (descriptor == null) {
            descriptor = bitmapDescriptorForCluster(clusterSize = cluster.size)
            descriptor?.let { clusterIconCache.put(cacheKey, it) }
        }

        descriptor?.let {
            markerOptions.icon(it)
            markerOptions.anchor(0.5f, 0.5f)
        }

        markerOptions.zIndex(5f)
    }

    private fun bitmapDescriptorForCluster(clusterSize: Int): BitmapDescriptor? {
        val density = mContext.resources.displayMetrics.density
        val textLength = clusterSize.toString().length
        val circleRadius = (18 * density + (textLength * 2 * density)).coerceAtMost(35 * density)

        val canvasSize = (circleRadius * 2 + 10 * density).toInt()
        val bitmap = createBitmap(canvasSize, canvasSize)
        val canvas = Canvas(bitmap)

        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#1976D2".toColorInt()
            setShadowLayer(4f, 0f, 2f, Color.argb(120, 0,0,0))
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2 * density
        }

        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)
        canvas.drawCircle(centerX, centerY, circleRadius, strokePaint)

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

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun bitmapDescriptorFromVector(resId: Int, isFavorite: Boolean): BitmapDescriptor? {
        val density = mContext.resources.displayMetrics.density
        val churchSize = (40 * density).toInt()
        val starSize = (16 * density).toInt()

        val maxPadding = 15 * density
        val canvasSize = (churchSize + (maxPadding * 2)).toInt()
        val bitmap = createBitmap(canvasSize, canvasSize)
        val canvas = Canvas(bitmap)

        val centerX = canvasSize / 2f
        val centerY = canvasSize / 2f

        val churchDrawable = ContextCompat.getDrawable(mContext, resId) ?: return null
        val cL = (centerX - churchSize / 2).toInt()
        val cT = (centerY - churchSize / 2).toInt()
        churchDrawable.setBounds(cL, cT, cL + churchSize, cT + churchSize)
        churchDrawable.draw(canvas)

        if (isFavorite) {
            drawStar(canvas, centerX + churchSize/3, centerY - churchSize/3, starSize/2f, density)
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
}