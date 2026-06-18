package com.example.mojaparafia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
var createIosInlineAdBannerView: (() -> UIView)? = null

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun InlineAdBanner(modifier: Modifier, isPremium: Boolean) {
    if (isPremium) return

    val factory = createIosInlineAdBannerView
    if (factory != null) {
        UIKitView(
            factory = factory,
            modifier = modifier.background(Color.White),
            update = { _ -> }
        )
    } else {
        Box(modifier = modifier)
    }
}