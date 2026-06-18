package com.example.mojaparafia

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import androidx.compose.ui.graphics.ImageBitmap

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun showPlatformToast(message: String)

expect fun navigateToMap(parishId: String, lat: Double, lon: Double)

@Composable
expect fun isLandscapeOrientation(): Boolean

@Composable
expect fun AdBannerView(modifier: Modifier = Modifier)

expect fun generateAndShareIntentionImage(text: String, backgroundBitmap: ImageBitmap)