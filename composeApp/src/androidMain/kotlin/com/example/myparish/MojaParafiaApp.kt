package com.example.myparish

import android.app.Application
import com.example.myparish.db.appContext // Twój globalny kontekst

class MojaParafiaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Łapiemy kontekst aplikacji, który przyda się do bazy Room i szyfrowania
        appContext = this
    }
}