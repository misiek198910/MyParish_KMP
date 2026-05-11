package com.example.myparish.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.example.myparish.billing.SubscriptionDao
import com.example.myparish.billing.SubscriptionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [ParishEntity::class, SubscriptionEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun parishDao(): ParishDao
    abstract fun subscriptionDao(): SubscriptionDao
}

// Funkcja pomocnicza do budowania bazy we wspólnym kodzie
fun createRoomDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver()) // Używa dołączonego silnika SQLite (ważne dla iOS)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

expect fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>