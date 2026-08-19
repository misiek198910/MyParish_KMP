package com.example.mojaparafia.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "parish_events")
data class ParishEventEntity(
    @PrimaryKey
    @SerialName("id")
    val id: Long,


    @SerialName("parishId")
    val parishId: String,

    @SerialName("eventDate")
    val eventDate: String,

    @SerialName("title")
    val title: String,

    @SerialName("description")
    val description: String? = null
)