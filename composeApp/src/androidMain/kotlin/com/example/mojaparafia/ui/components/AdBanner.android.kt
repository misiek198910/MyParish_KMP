package com.example.mojaparafia.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.example.mojaparafia.BuildConfig

@Composable
actual fun AdBanner(modifier: Modifier, isPremium: Boolean) {
    if (isPremium) return
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    val density = resources.displayMetrics.density
                    val adWidth = (resources.displayMetrics.widthPixels / density).toInt()
                    setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(ctx, adWidth))
                    adUnitId = BuildConfig.AD_BANNER_ID
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}