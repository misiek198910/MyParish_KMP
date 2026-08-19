package com.example.mojaparafia.ui.components

import android.content.Intent
import androidx.core.net.toUri
import com.example.mojaparafia.db.appContext

actual fun openMapNavigation(address: String) {
    val gmmIntentUri = "google.navigation:q=$address".toUri()
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    appContext.startActivity(mapIntent)
}