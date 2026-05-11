package com.example.myparish.repository

import com.example.myparish.db.DatabaseInstance
import com.example.myparish.db.ParishEntity
import com.example.myparish.network.apiService

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ParishRepository {

    // Zamiast LiveData używamy StateFlow - to nowy standard w KMP
    private val _parishes = MutableStateFlow<List<ParishEntity>>(emptyList())
    val parishes: StateFlow<List<ParishEntity>> = _parishes.asStateFlow()
    private val parishDao = DatabaseInstance.getDatabase().parishDao()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ==========================================
    // LOGIKA Z SYNC WORKER (Pobieranie parafii)
    // ==========================================
    suspend fun syncParishes(since: String? = null): Boolean {
        _isLoading.value = true
        return try {
            val fetchedParishes = apiService.getParishes(since)
            if (fetchedParishes != null) {
                // Tutaj w przyszłości dodamy zapis do lokalnej bazy danych (Room KMP)
                // Na ten moment po prostu trzymamy je w pamięci:
                _parishes.value = fetchedParishes
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            _isLoading.value = false
        }
    }

    // ==========================================
    // LOGIKA Z PROPOSAL WORKER (Wysyłanie zmian)
    // ==========================================
    suspend fun syncProposal(changeMap: Map<String, String>): Boolean {
        return try {
            // Sprawdzamy, czy to nowa parafia, czy tylko edycja
            val isNewParish = changeMap["is_new_parish"] == "true"

            val success = if (isNewParish) {
                apiService.submitNewParish(changeMap)
            } else {
                apiService.submitProposal(changeMap)
            }

            success // Jeśli true, Worker będzie wiedział, że ma zwrócić Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            false // Jeśli false, Worker zwróci Result.retry()
        }
    }
}

// Globalna instancja repozytorium do użycia w ViewModelach
val parishRepository = ParishRepository()