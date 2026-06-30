package com.example.mojaparafia.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mojaparafia.db.ParishEntity
import com.example.mojaparafia.model.AddIntentionRequest
import com.example.mojaparafia.model.DeleteIntentionRequest
import com.example.mojaparafia.model.ExtinguishRequest
import com.example.mojaparafia.model.Intention
import com.example.mojaparafia.model.LightCandleRequest
import com.example.mojaparafia.model.NewsResponse
import com.example.mojaparafia.model.PinRequest
import com.example.mojaparafia.model.PrayRequest
import com.example.mojaparafia.model.RenewRequest
import com.example.mojaparafia.model.SetHomeParishRequest
import com.example.mojaparafia.model.UpdateIntentionRequest
import com.example.mojaparafia.model.UpdateTokenRequest
import com.example.mojaparafia.network.apiService
import com.example.mojaparafia.repository.parishRepository
import com.example.mojaparafia.util.Reminder
import com.example.mojaparafia.util.ReminderScheduler
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ParishListViewModel : ViewModel() {

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    private val settings = Settings()

    private val REMINDERS_KEY = "saved_reminders_list"

    enum class LocationAction { CURRENT_LOCATION, NEAREST_PARISH }

    private val _remindersList = MutableStateFlow<List<Reminder>>(emptyList())
    val remindersList: StateFlow<List<Reminder>> = _remindersList.asStateFlow()

    private val _nearestParishesState =
        MutableStateFlow<Triple<List<ParishEntity>, Double, Double>?>(null)
    val nearestParishesState: StateFlow<Triple<List<ParishEntity>, Double, Double>?> =
        _nearestParishesState.asStateFlow()

    private val _locationRequest = MutableStateFlow<LocationAction?>(null)
    val locationRequest: StateFlow<LocationAction?> = _locationRequest.asStateFlow()
    private val _isFirstRun = MutableStateFlow(settings.getBoolean("is_first_run", true))
    val isFirstRun: StateFlow<Boolean> = _isFirstRun.asStateFlow()

    private val _isInitialSyncing = MutableStateFlow(false)
    val isInitialSyncing: StateFlow<Boolean> = _isInitialSyncing.asStateFlow()

    val allParishes: StateFlow<List<ParishEntity>> = parishRepository.parishes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _newsList = MutableStateFlow<List<NewsResponse>>(emptyList())
    val newsList: StateFlow<List<NewsResponse>> = _newsList.asStateFlow()

    private val _intentions = MutableStateFlow<List<Intention>>(emptyList())
    val intentions: StateFlow<List<Intention>> = _intentions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _homeParishId = MutableStateFlow<String?>(null)
    val homeParishId: StateFlow<String?> = _homeParishId.asStateFlow()

    private val _userPoints = MutableStateFlow(0)
    val userPoints: StateFlow<Int> = _userPoints.asStateFlow()

    private val _hasCrown = MutableStateFlow(false)
    val hasCrown: StateFlow<Boolean> = _hasCrown.asStateFlow()

    lateinit var deviceIdStr: String

    private val _mapFocusRequest = MutableStateFlow<Pair<Double, Double>?>(null)
    val mapFocusRequest: StateFlow<Pair<Double, Double>?> = _mapFocusRequest.asStateFlow()

    init {
        if (!isFirstRun.value) {
            viewModelScope.launch(Dispatchers.Default) {
                parishRepository.loadParishesFromDatabase()
                syncParishes()

                if (_homeParishId.value == null) {
                    val location = apiService.getIpLocation()
                    if (location != null && location.lat != 0.0 && location.lon != 0.0) {
                        withContext(Dispatchers.Main) {
                            focusMapOn(location.lat, location.lon)
                        }
                    }
                }
            }
        }
    }

    fun updatePremiumStatus(hasPremium: Boolean) {
        _isPremium.value = hasPremium
    }

    fun performInitialSyncAndFinish(onError: () -> Unit) {
        if (!_isFirstRun.value) return
        viewModelScope.launch(Dispatchers.Default) {
            _isInitialSyncing.value = true
            try {
                val success = parishRepository.syncParishes(forceFullSync = true)

                withContext(Dispatchers.Main) {
                    if (success) {
                        settings.putBoolean("is_first_run", false)
                        _isFirstRun.value = false
                    } else {
                        onError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isInitialSyncing.value = false
                }
            }
        }
    }

    fun getParishById(id: String): Flow<ParishEntity?> {
        return allParishes.map { list -> list.find { it.id == id } }
    }

    fun submitPriestRequest(parishId: String, email: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                 //apiService.sendCollaborationRequest(parishId, CollaborationRequest(email))
            } catch (e: Exception) {
            }
        }
    }

    fun fetchIntentions() {
        if (_deviceId.value.isEmpty()) return
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.value = true
            try {
                val intentList = apiService.getIntentions(_deviceId.value)
                if (intentList != null) {
                    _intentions.value = intentList
                }
            } catch (e: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addIntention(content: String, category: String, isAnonymous: Boolean, countryCode: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val request = AddIntentionRequest(
                    content,
                    _deviceId.value,
                    category,
                    isAnonymous,
                    countryCode
                )
                val success = apiService.addIntention(request)

                withContext(Dispatchers.Main) {
                    if (success) {
                        fetchIntentions()
                    }
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun updateIntention(intentionId: Int, content: String, category: String, isAnonymous: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val request = UpdateIntentionRequest(
                    intentionId,
                    _deviceId.value,
                    content,
                    category,
                    isAnonymous
                )
                val success = apiService.updateIntention(request)

                withContext(Dispatchers.Main) {
                    if (success) fetchIntentions()
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteIntention(intentionId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val request = DeleteIntentionRequest(id = intentionId, deviceId = _deviceId.value)
                if (apiService.deleteIntention(request)) {
                    fetchIntentions()
                    syncParishes()
                }
            } catch (e: Exception) {
            }
        }
    }

    fun prayForIntention(intentionId: Int) {
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

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val request = PrayRequest(intentionId, _deviceId.value)
                apiService.registerPrayer(request)
                fetchIntentions()
            } catch (e: Exception) {
                fetchIntentions()
            }
        }
    }

    fun togglePin(intentionId: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (apiService.pinIntention(PinRequest(intentionId, _deviceId.value))) {
                    fetchIntentions()
                }
            } catch (e: Exception) {
            }
        }
    }

    fun renewIntention(intentionId: Int, days: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val success =
                    apiService.renewIntention(RenewRequest(intentionId, _deviceId.value, days))
                withContext(Dispatchers.Main) { onResult(success) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun lightCandle(intentionId: Int, candleType: String, durationHours: Int, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val request =
                    LightCandleRequest(intentionId, _deviceId.value, candleType, durationHours)
                val success = apiService.lightCandle(request)

                withContext(Dispatchers.Main) {
                    if (success) {
                        fetchIntentions()
                    }
                    onComplete(success)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun extinguishCandle(candleId: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val success =
                    apiService.extinguishCandle(ExtinguishRequest(candleId, _deviceId.value))
                withContext(Dispatchers.Main) { onResult(success) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false) }
            }
        }
    }

    fun syncParishes() {
        viewModelScope.launch(Dispatchers.Default) {
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
        viewModelScope.launch(Dispatchers.Default) {
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
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val stats = apiService.getUserStats(_deviceId.value)
                if (stats != null) {
                    withContext(Dispatchers.Main) {
                        _userPoints.value = stats.points
                        _hasCrown.value = stats.hasCrown
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun toggleHomeParish(parishId: String, onNewHomeSaved: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val currentHomeId = _homeParishId.value
                val newParishId = if (currentHomeId == parishId) null else parishId

                val success =
                    apiService.setHomeParish(SetHomeParishRequest(_deviceId.value, newParishId))
                if (success) {
                    _homeParishId.value = newParishId

                    if (newParishId != null) {
                        settings.putString("home_parish_id", newParishId)
                    } else {
                        settings.remove("home_parish_id")
                    }

                    withContext(Dispatchers.Main) { onNewHomeSaved(newParishId) }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun sendFavoriteEventToServer(parishId: String, parishName: String?) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                apiService.logFavoriteAction(parishId, parishName, "added")
            } catch (e: Exception) {
            }
        }
    }

    fun sendUnfavoriteEventToServer(parishId: String, parishName: String = "Unknown") {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                apiService.logFavoriteAction(parishId, parishName, "removed")
            } catch (e: Exception) {
            }
        }
    }

    fun toggleFavorite(parish: ParishEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            val isNowFavorite = !parish.isFavorite
            parishRepository.updateParish(parish.copy(isFavorite = isNowFavorite))

            if (isNowFavorite) {
                sendFavoriteEventToServer(parish.id, parish.name)
            } else {
                sendUnfavoriteEventToServer(parish.id)
            }
        }
    }

    fun requestCurrentLocation() {
        _locationRequest.value = LocationAction.CURRENT_LOCATION
    }

    fun findNearestParish() {
        _locationRequest.value = LocationAction.NEAREST_PARISH
    }

    fun addReminder(scheduler: ReminderScheduler, parish: ParishEntity, massTime: String, triggerDateTime: kotlinx.datetime.LocalDateTime) {
        val newReminder = Reminder(
            notificationId = (kotlinx.datetime.Clock.System.now().toEpochMilliseconds() % Int.MAX_VALUE).toInt(),
            parishId = parish.id,
            parishName = parish.name ?: "Parafia",
            massTime = massTime,
            reminderDateTime = triggerDateTime
        )

        scheduler.scheduleReminder(newReminder)

        val currentList = getSavedReminders().toMutableList()
        currentList.add(newReminder)
        saveRemindersToStorage(currentList)

        loadReminders()
    }

    fun removeReminder(scheduler: ReminderScheduler, notificationId: Int) {
        scheduler.cancelReminder(notificationId)
        val currentList = getSavedReminders().toMutableList()
        currentList.removeAll { it.notificationId == notificationId }
        saveRemindersToStorage(currentList)
        loadReminders()
    }

    fun loadReminders() {
        _remindersList.value = getSavedReminders().sortedBy { it.reminderDateTime }
    }

    private fun getSavedReminders(): List<Reminder> {
        val jsonString = settings.getStringOrNull(REMINDERS_KEY)
        return if (jsonString.isNullOrEmpty()) {
            emptyList()
        } else {
            try {
                Json.decodeFromString<List<Reminder>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun saveRemindersToStorage(list: List<Reminder>) {
        val jsonString = Json.encodeToString(list)
        settings.putString(REMINDERS_KEY, jsonString)
    }

    fun onLocationRequestHandled() {
        _locationRequest.value = null
    }

    fun focusMapOn(lat: Double, lon: Double) {
        _mapFocusRequest.value = Pair(lat, lon)
    }

    fun onMapFocused() {
        _mapFocusRequest.value = null
    }

    fun logSearchEvent(query: String) {}
    fun clearNearestParishesState() {
        _nearestParishesState.value = null
    }

    fun processUserLocation(lat: Double, lon: Double, action: LocationAction) {

        viewModelScope.launch(Dispatchers.Default) {
            when (action) {
                LocationAction.CURRENT_LOCATION -> {
                    withContext(Dispatchers.Main) { focusMapOn(lat, lon) }
                }
                LocationAction.NEAREST_PARISH -> {
                    val parishesList = allParishes.value
                    if (parishesList.isNotEmpty()) {

                        val sorted = parishesList.sortedBy { calculateDistance(lat, lon, it.latitude, it.longitude) }
                        val nearest = sorted.first()

                        withContext(Dispatchers.Main) {
                            focusMapOn(nearest.latitude, nearest.longitude)
                            _nearestParishesState.value = Triple(sorted, lat, lon)
                        }
                    }
                }
            }
        }
    }
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = lat1 * kotlin.math.PI / 180
        val phi2 = lat2 * kotlin.math.PI / 180
        val deltaPhi = (lat2 - lat1) * kotlin.math.PI / 180
        val deltaLambda = (lon2 - lon1) * kotlin.math.PI / 180

        val a = kotlin.math.sin(deltaPhi / 2) * kotlin.math.sin(deltaPhi / 2) +
                kotlin.math.cos(phi1) * kotlin.math.cos(phi2) *
                kotlin.math.sin(deltaLambda / 2) * kotlin.math.sin(deltaLambda / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))

        return r * c
    }

    fun initFromPlatform(deviceIdStr: String, savedHomeParishId: String?) {
        _deviceId.value = deviceIdStr
        this.deviceIdStr =
            deviceIdStr
        _homeParishId.value = savedHomeParishId

        if (deviceIdStr.isNotEmpty()) {
            fetchUserStats()
            fetchIntentions()
            fetchNews()
        }
    }

    fun submitParishProposal(parishData: Map<String, String>) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val dataToSend = parishData.toMutableMap()

                if (!dataToSend.containsKey("device_id") && _deviceId.value.isNotEmpty()) {
                    dataToSend["device_id"] = _deviceId.value
                }

                val isNewParish = dataToSend["is_new_parish"] == "true"

                val response = if (isNewParish) {
                    httpClient.post("https://api-parafia.mivs.dev/api/new-parish") {
                        contentType(ContentType.Application.Json)
                        setBody(dataToSend)
                    }
                } else {
                    httpClient.post("https://api-parafia.mivs.dev/proposals") {
                        setBody(FormDataContent(Parameters.build {
                            dataToSend.forEach { (key, value) ->
                                append(key, value)
                            }
                        }))
                    }
                }

                if (response.status.isSuccess()) {
                    println("Sukces! Zmiana zapisana na serwerze.")
                } else {
                    println("Błąd serwera: ${response.status} - ${response.bodyAsText()}")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println("Błąd połączenia: ${e.message}")
            }
        }
    }

    fun saveFcmToken(token: String) {

        if (_deviceId.value.isEmpty() || token.isEmpty()) {
            println("[FCM] Błąd: Brak deviceId lub tokena. Nie można wysłać do serwera.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = UpdateTokenRequest(
                    deviceId = _deviceId.value,
                    fcmToken = token
                )

                val isSuccess = apiService.updateFcmToken(request)

                if (isSuccess) {
                    println("[FCM] Token wysłany pomyślnie: $token")
                } else {
                    println("[FCM] Błąd: Serwer odrzucił aktualizację tokena (status: false).")
                }
            } catch (e: Exception) {
                println("[FCM] Krytyczny błąd podczas aktualizacji tokena na serwerze: ${e.message}")
                e.printStackTrace()
            }
        }
    }


}