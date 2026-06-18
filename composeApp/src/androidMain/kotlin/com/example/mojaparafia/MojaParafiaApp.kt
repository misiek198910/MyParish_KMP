package com.example.mojaparafia

import android.app.Application
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.example.mojaparafia.db.appContext // Twój globalny kontekst

class MojaParafiaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        appContext = this

    }
}