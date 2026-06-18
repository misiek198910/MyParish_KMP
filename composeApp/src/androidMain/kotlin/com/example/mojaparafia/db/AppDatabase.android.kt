package com.example.mojaparafia.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

// Statyczna zmienna, którą zainicjalizujemy w klasie MojaParafiaApp
lateinit var appContext: Context

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = appContext.getDatabasePath("moja_parafia.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}