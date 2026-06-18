package com.example.mojaparafia.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mojaparafia.R
import com.example.mojaparafia.MainActivity


class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)
        val parishAddress = intent.getStringExtra("parish_address")
        val parishName = intent.getStringExtra("parish_name")
        val massTime = intent.getStringExtra("mass_time")

        Log.d("AlarmReceiver", "Odebrano alarm dla: $parishName o $massTime, ID: $notificationId")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notif_channel_desc)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mapIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            putExtra("parish_address", parishAddress)
            putExtra("notification_id", notificationId)
        }

        val mapPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            // UWAGA: Jeśli nie przeniosłeś jeszcze obrazków, podmień to tymczasowo na np. android.R.drawable.ic_dialog_info
            .setSmallIcon(R.drawable.ic_notification_vector)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.image_church))
            .setContentTitle(context.getString(R.string.notif_title, parishName ?: ""))
            .setContentText(context.getString(R.string.notif_text, massTime ?: "", parishAddress ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            // Używamy bezpieczniejszego wywołania koloru dla starszych Androidów
            .addAction(
                android.R.drawable.ic_menu_directions,
                context.getString(R.string.notif_action_navigate),
                mapPendingIntent
            )
            .setStyle(NotificationCompat.BigTextStyle()
                .setBigContentTitle(context.getString(R.string.notif_title, parishName ?: ""))
                .bigText("${context.getString(R.string.notif_text, massTime ?: "", parishAddress ?: "")}\n\n${context.getString(R.string.notif_blessing)}"))

        notificationManager.notify(notificationId, builder.build())
        Log.d("AlarmReceiver", "Wysłano powiadomienie o ID: $notificationId")
    }

    companion object {
        private const val CHANNEL_ID = "mass_reminder_channel"
    }
}