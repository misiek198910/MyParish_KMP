package com.example.mojaparafia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
var createIosAdBannerView: (() -> UIView)? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun AdBanner(modifier: Modifier, isPremium: Boolean) {
    if (isPremium) return

    val factory = createIosAdBannerView
    if (factory != null) {
        UIKitView(
            factory = factory,
            modifier = modifier.height(50.dp).background(Color.White),
            update = { _ -> }
        )
    } else {
        Box(modifier = modifier.height(50.dp))
    }
}