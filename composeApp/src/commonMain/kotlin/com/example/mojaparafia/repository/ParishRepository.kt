package com.example.mojaparafia.repository

import com.example.mojaparafia.db.DatabaseInstance
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.network.apiService
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.TimeZone

class ParishRepository {

    private var hasSyncedThisSession = false
    private val _parishes = MutableStateFlow<List<ParishEntity>>(emptyList())
    private val parishDao = DatabaseInstance.getDatabase().parishDao()
    private val settings = Settings()
    private val LAST_SYNC_KEY = "last_sync_timestamp"

    val parishes: Flow<List<ParishEntity>> = parishDao.getAllParishes()

    suspend fun syncParishes(forceFullSync: Boolean = false): Boolean {

        if (hasSyncedThisSession && !forceFullSync) return true

        return try {
            val since = if (forceFullSync) null else settings.getStringOrNull(LAST_SYNC_KEY)
            val fetchedParishes = apiService.getParishes(since) { _ -> }

            if (fetchedParishes != null) {
                if (fetchedParishes.isNotEmpty()) {
                    val currentLocalParishes = parishDao.getAllParishes().first()
                    val mergedParishes = fetchedParishes.map { serverParish ->
                        val localParish = currentLocalParishes.find { it.id == serverParish.id }
                        if (localParish != null) {
                            serverParish.copy(isFavorite = localParish.isFavorite)
                        } else {
                            serverParish
                        }
                    }
                    parishDao.upsertParishes(mergedParishes)
                    _parishes.value = mergedParishes
                } else {
                    if (_parishes.value.isEmpty()) {
                        loadParishesFromDatabase()
                    }
                }

                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val sqlTimestamp = "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"

                settings.putString(LAST_SYNC_KEY, sqlTimestamp)

                hasSyncedThisSession = true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun loadParishesFromDatabase() {
        try {
            val localData = parishDao.getAllParishes().first()
            _parishes.value = localData
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateParish(parish: ParishEntity) {
        parishDao.update(parish)
    }
}

val parishRepository = ParishRepository()