package com.example.mojaparafia

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers

// Globalny uchwyt do Contextu dla funkcji bez @Composable
@SuppressLint("StaticFieldLeak")
object AndroidAppContext {
    lateinit var context: Context
    fun initialize(ctx: Context) {
        context = ctx
    }
}

actual fun showPlatformToast(message: String) {
    Toast.makeText(AndroidAppContext.context, message, Toast.LENGTH_SHORT).show()
}

actual fun navigateToMap(parishId: String, lat: Double, lon: Double) {
    val ctx = AndroidAppContext.context
    val intent = Intent(ctx, MainActivity::class.java).apply {
        putExtra("TARGET_PARISH_ID", parishId)
        putExtra("TARGET_LAT", lat)
        putExtra("TARGET_LON", lon)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
    }
    ctx.startActivity(intent)
}

@Composable
actual fun isLandscapeOrientation(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

@Composable
actual fun AdBannerView(modifier: Modifier) {
    val context = LocalContext.current
    val isLandscape = isLandscapeOrientation()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            AdView(ctx).apply {
                // W KMP wymuszamy bezpieczny rozmiar dla reklam
                setAdSize(if (isLandscape) AdSize.BANNER else AdSize.FULL_BANNER)
                adUnitId = "TU_WPISZ_SWOJ_KLUCZ_BANNERA" // Zastąp zmienną środowiskową BuildConfig
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@OptIn(ExperimentalResourceApi::class)
actual fun generateAndShareIntentionImage(text: String, backgroundBitmap: ImageBitmap) {
    val ctx = AndroidAppContext.context

    // Używamy Dispatchers.Default, bo to operacja graficzna, nie chcemy blokować UI
    MainScope().launch(Dispatchers.Default) {
        try {
            // 🔥 Natychmiastowa konwersja do natywnej Androidowej Bitmapy!
            val srcBitmap = backgroundBitmap.asAndroidBitmap()
            val bitmap = srcBitmap.scale(1080, 1080).copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap)

            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), Paint().apply { color =
                "#99000000".toColorInt() })

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(15f, 0f, 0f, android.graphics.Color.BLACK)
            }

            val margin = 150
            var currentSize = 80f
            var layout: StaticLayout

            do {
                textPaint.textSize = currentSize
                layout = StaticLayout.Builder.obtain("„$text”", 0, "„$text”".length, textPaint, canvas.width - (margin * 2))
                    .setAlignment(Layout.Alignment.ALIGN_CENTER)
                    .setLineSpacing(0f, 1.25f)
                    .setIncludePad(false).build()
                currentSize -= 2f
            } while (layout.height > canvas.height - 400 && currentSize > 28f)

            canvas.save()
            canvas.translate(margin.toFloat(), (canvas.height - layout.height) / 2f - 40f)
            layout.draw(canvas)
            canvas.restore()

            val file = File(File(ctx.cacheDir, "shared_images").apply { mkdirs() }, "intencja.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

            val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(Intent.createChooser(intent, "Udostępnij intencję").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

        } catch (e: Exception) {
            // W Androidzie wywołanie Toasta musi wrócić na główny wątek!
            launch(Dispatchers.Main) {
                Toast.makeText(ctx, "Błąd generowania grafiki", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()