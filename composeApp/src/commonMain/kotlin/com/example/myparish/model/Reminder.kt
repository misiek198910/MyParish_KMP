package com.example.myparish.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val notificationId: Int,
    val parishId: String?,
    val parishName: String?,
    val parishAddress: String?,
    val massTime: String?,
    // Używamy wieloplatformowego LocalDateTime z kotlinx-datetime
    val reminderDateTime: LocalDateTime?,
    val minutesBefore: Int
) {
    override fun toString(): String {
        return "Reminder{" +
                "notificationId=" + notificationId +
                ", parishName='" + parishName + '\'' +
                ", massTime='" + massTime + '\'' +
                ", reminderDateTime=" + reminderDateTime +
                ", minutesBefore=" + minutesBefore +
                '}'
    }
}