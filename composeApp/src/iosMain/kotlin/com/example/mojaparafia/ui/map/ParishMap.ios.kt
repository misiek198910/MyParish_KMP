package com.example.mojaparafia.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import com.example.mojaparafia.db.ParishEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.isSystemInDarkTheme

interface SwiftMapController {
    val view: platform.UIKit.UIView
    fun updateParishes(jsonString: String)
    fun centerOn(lat: Double, lng: Double)
    fun setOnMarkerClickListener(onClick: (String) -> Unit)
    fun setOnCameraChangeListener(onChange: (String) -> Unit)
    fun setMapTheme(isDark: Boolean)
}

interface SwiftMapFactory {
    fun createMap(): SwiftMapController
}

object ParishMapBridge {
    var globalSwiftMapFactory: SwiftMapFactory? = null
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun ParishMap(
    modifier: Modifier,
    parishes: List<ParishEntity>,
    focusRequest: Pair<Double, Double>?,
    onMapFocused: () -> Unit,
    homeParishId: String?,
    userHasCrown: Boolean,
    onMapLoaded: () -> Unit,
    onMarkerClick: (parishId: String) -> Unit,
    onMapLongClick: (lat: Double, lng: Double) -> Unit
) {
    val mapController = remember { ParishMapBridge.globalSwiftMapFactory?.createMap() }

    if (mapController == null) {
        Text("Błąd: Natywny silnik mapy iOS nie został zainicjowany w Xcode.")
        return
    }

    var boundingBox by remember { mutableStateOf<DoubleArray?>(null) }

    val isMapDark = isSystemInDarkTheme()

    LaunchedEffect(isMapDark) {
        mapController.setMapTheme(isMapDark)
    }

    LaunchedEffect(Unit) {
        mapController.setOnMarkerClickListener { id ->
            onMarkerClick(id)
        }
        mapController.setOnCameraChangeListener { boundsString ->
            val parts = boundsString.split(",")
            if (parts.size == 4) {
                val minLat = parts[0].toDoubleOrNull() ?: return@setOnCameraChangeListener
                val maxLat = parts[1].toDoubleOrNull() ?: return@setOnCameraChangeListener
                val minLng = parts[2].toDoubleOrNull() ?: return@setOnCameraChangeListener
                val maxLng = parts[3].toDoubleOrNull() ?: return@setOnCameraChangeListener

                boundingBox = doubleArrayOf(minLat, maxLat, minLng, maxLng)
            }
        }
        onMapLoaded()
    }

    LaunchedEffect(parishes, homeParishId, userHasCrown, boundingBox) {
        if (parishes.isEmpty()) return@LaunchedEffect

        val jsonString = withContext(Dispatchers.Default) {
            val parishesToDisplay = if (boundingBox != null) {
                val (minLat, maxLat, minLng, maxLng) = boundingBox!!
                val visibleParishes = parishes.filter {
                    it.latitude in minLat..maxLat && it.longitude in minLng..maxLng
                }
                if (visibleParishes.size > 300) {
                    val step = visibleParishes.size / 300
                    visibleParishes.filterIndexed { index, _ -> index % step == 0 }.take(300)
                } else {
                    visibleParishes
                }
            } else {
                val home = parishes.find { it.id == homeParishId }
                val initialList = parishes.take(100).toMutableList()
                if (home != null && !initialList.contains(home)) {
                    initialList.add(0, home)
                }
                initialList
            }

            buildString {
                append("[")
                parishesToDisplay.forEachIndexed { index, p ->
                    val isHome = p.id.toString() == homeParishId
                    val glyph = when {
                        isHome && userHasCrown -> "👑"
                        p.isCathedral -> "🕍"
                        else -> "⛪"
                    }
                    val subtitle = buildString {
                        if (p.active_intentions > 0) append("🙏 ${p.active_intentions}   ")
                        if (p.active_candles > 0) append("🕯️ ${p.active_candles}")
                    }.trim()

                    val safeTitle = (p.name ?: "").replace("\"", "\\\"").replace("\n", " ")
                    val safeSubtitle = subtitle.replace("\"", "\\\"").replace("\n", " ")

                    append("{")
                    append("\"id\":\"${p.id}\",")
                    append("\"lat\":${p.latitude},")
                    append("\"lng\":${p.longitude},")
                    append("\"title\":\"$safeTitle\",")
                    append("\"subtitle\":\"$safeSubtitle\",")
                    append("\"hasExtras\":${p.active_candles > 0 || p.active_intentions > 0},")
                    append("\"hasCandles\":${p.active_candles > 0},")
                    append("\"glyphText\":\"$glyph\"")
                    append("}")

                    if (index < parishesToDisplay.size - 1) append(",")
                }
                append("]")
            }
        }
        mapController.updateParishes(jsonString)
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let { (lat, lon) ->
            mapController.centerOn(lat, lon)
            onMapFocused()
        }
    }

    UIKitView(
        modifier = modifier.fillMaxSize(),
        factory = { mapController.view },
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative
        ),
        update = {}
    )
}