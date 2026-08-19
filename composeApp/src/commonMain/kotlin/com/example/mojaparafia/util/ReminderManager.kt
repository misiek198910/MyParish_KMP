package com.example.mojaparafia.util

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val notificationId: Int,
    val parishId: String,
    val parishName: String,
    val massTime: String,
    val reminderDateTime: LocalDateTime
)

interface ReminderScheduler {
    fun scheduleReminder(reminder: Reminder)
    fun cancelReminder(notificationId: Int)
    fun getActiveReminders(): List<Reminder>
}