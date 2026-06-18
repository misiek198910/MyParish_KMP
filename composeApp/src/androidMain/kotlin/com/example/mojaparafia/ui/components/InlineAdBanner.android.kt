package com.example.mojaparafia.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.example.mojaparafia.BuildConfig

@Composable
actual fun InlineAdBanner(modifier: Modifier, isPremium: Boolean) {
    if (isPremium) return
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(250.dp),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.MEDIUM_RECTANGLE)
                    adUnitId = BuildConfig.AD_BANNER_INLINE_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}