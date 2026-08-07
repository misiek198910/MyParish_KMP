package com.example.mojaparafia.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ParishEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<ParishEventEntity>)

    @Query("SELECT * FROM parish_events WHERE parishId = :parishId ORDER BY eventDate ASC")
    fun getEventsForParish(parishId: String): Flow<List<ParishEventEntity>>

    @Query("DELETE FROM parish_events WHERE parishId = :parishId")
    suspend fun deleteEventsForParish(parishId: String)

    @Query("DELETE FROM parish_events")
    suspend fun deleteAllEvents()
}