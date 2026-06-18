package com.example.mojaparafia.ui.map

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.ui.components.ParishClusterItem
import com.example.mojaparafia.ui.components.ParishRenderer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.delay
import com.example.mojaparafia.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun ParishMap(
    modifier: Modifier,
    parishes: List<ParishEntity>,
    focusRequest: Pair<Double, Double>?,
    onMapFocused: () -> Unit,
    homeParishId: String?,
    userHasCrown: Boolean,
    onMapLoaded: () -> Unit,
    onMarkerClick: (String) -> Unit,
    onMapLongClick: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var mMap by remember { mutableStateOf<GoogleMap?>(null) }
    var clusterManager by remember { mutableStateOf<ClusterManager<ParishClusterItem>?>(null) }
    var isInitialCentered by rememberSaveable { mutableStateOf(false) }
    var mapReady by remember { mutableStateOf(false) }

    // 🔥 NOWOŚĆ: Odczytujemy ustawienia bez polegania na zawodnym Android OS
    val kmpSettings = remember { com.russhwolf.settings.Settings() }
    val themeMode = remember { kmpSettings.getInt("app_theme", 0) }
    var isAutoDark by remember { mutableStateOf(false) }

    // 🕒 Obliczanie słońca co minutę, jeśli wybrano tryb automatyczny (0)
    LaunchedEffect(themeMode) {
        if (themeMode == 0) {
            while (true) {
                val calendar = java.util.Calendar.getInstance()
                val month = calendar.get(java.util.Calendar.MONTH)
                val time = calendar.get(java.util.Calendar.HOUR_OF_DAY) + calendar.get(java.util.Calendar.MINUTE) / 60.0

                val sunTimes = arrayOf(
                    8.0 to 15.75, 7.25 to 16.75, 6.25 to 17.75, 6.0 to 19.75,
                    5.0 to 20.75, 4.25 to 21.5, 4.75 to 21.25, 5.5 to 20.25,
                    6.5 to 19.0, 7.25 to 17.75, 7.25 to 15.75, 8.0 to 15.25
                )
                val (sunrise, sunset) = sunTimes[month]

                isAutoDark = time < sunrise || time >= sunset
                delay(60000) // Sprawdź ponownie za 60 sekund
            }
        }
    }

    // Ostateczna decyzja, czy mapa ma być ciemna
    val isMapDark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isAutoDark
    }

    LaunchedEffect(focusRequest) {
        focusRequest?.let { (lat, lon) ->
            while (mMap == null) delay(100)

            mMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 15f),
                1000,
                null
            )
            onMapFocused()
        }
    }

    val mapView = remember { MapView(context).apply { id = android.view.View.generateViewId() } }

    DisposableEffect(lifecycleOwner) {
        mapView.onCreate(Bundle())
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { googleMap ->
                    mMap = googleMap

                    if (isMapDark) {
                        try {
                            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark))
                        } catch (e: Exception) {
                            Log.e("MAP_STYLE", "Błąd stylu", e)
                        }
                    }

                    googleMap.uiSettings.apply {
                        isZoomControlsEnabled = false
                        isMapToolbarEnabled = false
                        isCompassEnabled = false
                    }

                    googleMap.setOnMapLongClickListener { latLng -> onMapLongClick(latLng.latitude, latLng.longitude) }


                    if (clusterManager == null) {
                        clusterManager = ClusterManager<ParishClusterItem>(context, googleMap)

                        val renderer = ParishRenderer(context, googleMap, clusterManager)
                        renderer.isNightMode = isMapDark
                        clusterManager?.renderer = renderer

                        googleMap.setOnMarkerClickListener(clusterManager)
                        googleMap.setOnCameraIdleListener(clusterManager)

                        clusterManager?.setOnClusterItemClickListener { item ->
                            item.parishId?.let { id ->
                                onMarkerClick(id)
                            }
                            true
                        }
                    }

                    mapReady = true
                }
            }
        },
        modifier = modifier.fillMaxSize(),
        update = {
            mMap?.let { map ->
                try {
                    // 🔥 ZMIANA: Wywołanie aktualizacji stylu na podstawie wyliczonej zmiennej `isMapDark`
                    if (isMapDark) {
                        map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark))
                    } else {
                        map.setMapStyle(null)
                    }
                } catch (e: Exception) {
                    Log.e("MAP_STYLE", "Błąd aktualizacji stylu", e)
                }

                (clusterManager?.renderer as? ParishRenderer)?.let { renderer ->
                    if (renderer.isNightMode != isMapDark) {
                        renderer.isNightMode = isMapDark
                        clusterManager?.cluster()
                    }
                }

            }
        }
    )

    LaunchedEffect(mapReady, parishes, homeParishId, userHasCrown) {
        if (mapReady && mMap != null) {
            val manager = clusterManager ?: return@LaunchedEffect

            delay(300)

            val newClusterItems = withContext(Dispatchers.Default) {
                parishes.map { parish ->
                    ParishClusterItem(
                        lat = parish.latitude,
                        lng = parish.longitude,
                        title = parish.name,
                        snippet = parish.address,
                        parishId = parish.id,
                        isCathedral = if (parish.isCathedral) 1 else 0,
                        isFavorite = parish.isFavorite,
                        activeIntentions = parish.active_intentions,
                        activeCandles = parish.active_candles,
                        isHomeParish = (parish.id == homeParishId),
                        userHasCrown = userHasCrown
                    )
                }
            }

            try {
                synchronized(manager) {
                    manager.clearItems()
                    manager.addItems(newClusterItems)
                    manager.cluster()
                }
            } catch (e: Exception) { Log.e("MAP_ERROR", "Błąd klastrów") }
        }
    }

    LaunchedEffect(mapReady, parishes, homeParishId) {
        if (mapReady && !isInitialCentered) {
            val map = mMap ?: return@LaunchedEffect
            val activity = context as? Activity

            val targetLat = activity?.intent?.getDoubleExtra("TARGET_LAT", 0.0) ?: 0.0
            val targetLon = activity?.intent?.getDoubleExtra("TARGET_LON", 0.0) ?: 0.0

            if (targetLat != 0.0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(targetLat, targetLon), 15f))
                isInitialCentered = true
                onMapLoaded()
            }
            else if (homeParishId != null && parishes.isNotEmpty()) {
                val homeParish = parishes.find { it.id == homeParishId }
                if (homeParish != null) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(homeParish.latitude, homeParish.longitude), 15f))
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(52.23, 21.01), 6f))
                }
                isInitialCentered = true
                onMapLoaded()
            }
            else if (homeParishId == null && parishes.isNotEmpty()) {
                isInitialCentered = true
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(52.23, 21.01), 6f))
                onMapLoaded()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            mMap?.clear()
            clusterManager?.clearItems()
            clusterManager = null
            mapView.onLowMemory()
        }
    }
}