package com.example.myparish.db

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFilePath = documentDirectory() + "/moja_parafia.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath,
        // ZMIANA: Bezpośrednie wywołanie wygenerowanej klasy _Impl
        factory = { AppDatabase_Impl() }
    )
}

private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    return documentDirectory?.path ?: ""
}