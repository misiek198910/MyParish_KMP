package com.example.mojaparafia.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mivs.mojaparafia.util.ReminderManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) { 

            Log.d("BootReceiver", "Wykryto restart systemu. Przywracanie alarmów...")

            val reminderManager = ReminderManager(context)
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val remindersToRestore = reminderManager.getActiveReminders()
                .filter { it.reminderDateTime > now }

            remindersToRestore.forEach { reminder ->
                reminderManager.scheduleReminder(reminder)
            }

            Log.d("BootReceiver", "Przywrócono ${remindersToRestore.size} alarmów.")
        }
    }
}