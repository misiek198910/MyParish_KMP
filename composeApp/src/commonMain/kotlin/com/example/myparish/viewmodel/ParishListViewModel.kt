package com.example.myparish.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myparish.model.*
import com.example.myparish.network.apiService
import com.example.myparish.repository.parishRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParishListViewModel : ViewModel() {

    // --- STAN (Odpowiedniki LiveData) ---

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsResponse>>(emptyList())
    val newsList: StateFlow<List<NewsResponse>> = _newsList.asStateFlow()

    private val _intentions = MutableStateFlow<List<Intention>>(emptyList())
    val intentions: StateFlow<List<Intention>> = _intentions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Będziemy to wstrzykiwać z platformy (np. Android_ID na Androidzie)
    // Na ten moment inicjalizujemy pustym, a ustawiamy metodą setDeviceId()
    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _homeParishId = MutableStateFlow<String?>(null)
    val homeParishId: StateFlow<String?> = _homeParishId.asStateFlow()

    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _hasCrown = MutableStateFlow(false)
    val hasCrown: StateFlow<Boolean> = _hasCrown.asStateFlow()

    private val _candleInventory = MutableStateFlow(CandleInventory(0, 0, 0))
    val candleInventory: StateFlow<CandleInventory> = _candleInventory.asStateFlow()

    // --- FILTRY ---

    // Tworzymy strukturę danych, aby trzymać wszystkie filtry w jednym strumieniu (Flow)
    // To znacznie lepsze w Compose niż pierdyliard osobnych zmiennych
    data class FilterState(
        val isCathedralActive: Boolean = false,
        val isChurchActive: Boolean = false,
        val isMassForChildrenActive: Boolean = false,
        val isVigilMassActive: Boolean = false,
        val isConfessionActive: Boolean = false,
        val isAdorationActive: Boolean = false,
        val isFirstSatActive: Boolean = false,
        val isFavoriteActive: Boolean = false,
        val isMultimediaActive: Boolean = false,
        val regionQuery: String = ""
    )

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()


    // --- INICJALIZACJA Z PLATFORMY ---
    fun initFromPlatform(
        deviceIdStr: String,
        savedHomeParishId: String?
    ) {
        _deviceId.value = deviceIdStr
        _homeParishId.value = savedHomeParishId

        // Ładujemy dane jak tylko poznamy ID
        if(deviceIdStr.isNotEmpty()) {
            fetchUserStats()
            fetchInventory()
            fetchIntentions()
            fetchNews()
        }
    }


    // =================================================================
    // METODY EKWIPUNKU
    // =================================================================

    fun fetchInventory() {
        if(_deviceId.value.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // W prawdziwym środowisku moglibyśmy dodać to do apiService
                // Na razie symulujemy:
                val inventory = CandleInventory() // TODO: Implementacja apiService.getInventory(deviceId)
                _candleInventory.value = inventory
            } catch (e: Exception) {
                // Log.e
            }
        }
    }

    fun addCandleToInventory(durationHours: Int, amount: Int = 1, onComplete: (Boolean) -> Unit) {
        if(_deviceId.value.isEmpty()) { onComplete(false); return }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // val request = AddInventoryRequest(_deviceId.value, durationHours, amount)
                // val result = apiService.addToInventory(request)
                // Wymaga zaktualizowania ParishApiService

                withContext(Dispatchers.Main) {
                    onComplete(true) // Symulacja na ten moment
                    fetchInventory()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    // =================================================================
    // INTENCJE
    // =================================================================

    fun fetchIntentions() {
        if(_deviceId.value.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val intentList = apiService.getIntentions(_deviceId.value)
                if (intentList != null) {
                    _intentions.value = intentList
                }
            } catch (e: Exception) {
                // Log.e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addIntention(content: String, category: String, isAnonymous: Boolean, countryCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = AddIntentionRequest(content, _deviceId.value, category, isAnonymous, countryCode)
                val success = apiService.addIntention(request)

                withContext(Dispatchers.Main) {
                    if (success) {
                        fetchIntentions()
                        syncParishes()
                    }
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun updateIntention(intentionId: Int, content: String, category: String, isAnonymous: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateIntentionRequest(intentionId, _deviceId.value, content, category, isAnonymous)
                val success = apiService.updateIntention(request)

                withContext(Dispatchers.Main) {
                    if(success) fetchIntentions()
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteIntention(intentionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = DeleteIntentionRequest(intentionId, _deviceId.value)
                if(apiService.deleteIntention(request)) {
                    fetchIntentions()
                    syncParishes()
                }
            } catch (e: Exception) {
            }
        }
    }

    fun prayForIntention(intentionId: Int) {
        // Optymistyczna aktualizacja UI (jak u Ciebie w starym kodzie)
        val currentList = _intentions.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == intentionId }

        if (index != -1) {
            val item = currentList[index]
            val isNowPraying = !item.prayedByMe
            val newCount = if (isNowPraying) item.prayerCount + 1 else item.prayerCount - 1
            currentList[index] = item.copy(
                prayedByMe = isNowPraying,
                prayerCount = newCount
            )
            _intentions.value = currentList
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = PrayRequest(intentionId, _deviceId.value)
                apiService.registerPrayer(request)
                fetchIntentions()
            } catch (e: Exception) {
                fetchIntentions() // Cofnij UI jeśli błąd
            }
        }
    }

    fun togglePin(intentionId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if(apiService.pinIntention(PinRequest(intentionId, _deviceId.value))) {
                    fetchIntentions()
                }
            } catch (e: Exception) {}
        }
    }

    fun renewIntention(intentionId: Int, days: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = apiService.renewIntention(RenewRequest(intentionId, _deviceId.value, days))
                withContext(Dispatchers.Main) { onResult(success) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // =================================================================
    // ŚWIECE
    // =================================================================

    fun lightCandle(intentionId: Int, candleType: String, durationHours: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = LightCandleRequest(intentionId, _deviceId.value, candleType, durationHours)
                val success = apiService.lightCandle(request)

                withContext(Dispatchers.Main) {
                    if (success) {
                        fetchIntentions()
                        fetchInventory()
                    }
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun extinguishCandle(candleId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = apiService.extinguishCandle(ExtinguishRequest(candleId, _deviceId.value))
                withContext(Dispatchers.Main) { onResult(success) }
            } catch(e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    // =================================================================
    // PARAFIE I INNE
    // =================================================================

    fun syncParishes() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                parishRepository.syncParishes()
            } catch (e: Exception) {
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun fetchNews() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val news = apiService.getNewsFeed()
                _newsList.value = news ?: emptyList()
            } catch (e: Exception) {
                _newsList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchUserStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val stats = apiService.getUserStats(_deviceId.value)
                if(stats != null) {
                    withContext(Dispatchers.Main) {
                        _userPoints.value = stats.points
                        _hasCrown.value = stats.hasCrown
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun toggleHomeParish(parishId: String, onNewHomeSaved: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentHomeId = _homeParishId.value
                val newParishId = if (currentHomeId == parishId) null else parishId

                val success = apiService.setHomeParish(SetHomeParishRequest(_deviceId.value, newParishId))
                if(success) {
                    _homeParishId.value = newParishId
                    // Przekazujemy na zewnątrz żeby zapisać w platformowym SharedPreferences
                    withContext(Dispatchers.Main) { onNewHomeSaved(newParishId) }
                }
            } catch (e: Exception) {}
        }
    }

    fun sendFavoriteEventToServer(parishId: String, parishName: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try { apiService.logFavoriteAction(parishId, parishName, "added") } catch (e: Exception) {}
        }
    }

    fun sendUnfavoriteEventToServer(parishId: String, parishName: String = "Unknown") {
        viewModelScope.launch(Dispatchers.IO) {
            try { apiService.logFavoriteAction(parishId, parishName, "removed") } catch (e: Exception) {}
        }
    }

    fun setCathedralFilterActive(active: Boolean) {
        _filterState.update { it.copy(isCathedralActive = active, isChurchActive = if(active) false else it.isChurchActive) }
    }
    fun setChurchFilterActive(active: Boolean) {
        _filterState.update { it.copy(isChurchActive = active, isCathedralActive = if(active) false else it.isCathedralActive) }
    }
    fun setMassForChildrenFilterActive(active: Boolean) { _filterState.update { it.copy(isMassForChildrenActive = active) } }
    fun setVigilMassFilterActive(active: Boolean) { _filterState.update { it.copy(isVigilMassActive = active) } }
    fun setConfessionFilterActive(active: Boolean) { _filterState.update { it.copy(isConfessionActive = active) } }
    fun setAdorationFilterActive(active: Boolean) { _filterState.update { it.copy(isAdorationActive = active) } }
    fun setFirstSatFilterActive(active: Boolean) { _filterState.update { it.copy(isFirstSatActive = active) } }
    fun setFavoriteFilterActive(active: Boolean) { _filterState.update { it.copy(isFavoriteActive = active) } }
    fun setMultimediaFilterActive(active: Boolean) { _filterState.update { it.copy(isMultimediaActive = active) } }
    fun setRegionFilter(query: String) { _filterState.update { it.copy(regionQuery = query) } }
}