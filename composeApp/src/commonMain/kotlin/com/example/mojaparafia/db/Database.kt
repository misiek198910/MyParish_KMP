package com.example.mojaparafia.db

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
object DatabaseInstance {
    private var instance: AppDatabase? = null

    fun getDatabase(): AppDatabase {
        return instance ?: createRoomDatabase(getDatabaseBuilder()).also { instance = it }
    }
}