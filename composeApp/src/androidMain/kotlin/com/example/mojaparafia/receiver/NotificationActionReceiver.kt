package com.example.mojaparafia.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val parishAddress = intent.getStringExtra("parish_address")

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notificationId)

        if (!parishAddress.isNullOrEmpty()) {
            val mapUri = "google.navigation:q=$parishAddress&mode=w".toUri()
            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(mapIntent)
            } catch (e: Exception) {
                // Ignorujemy brak map Google
            }
        }
    }
}