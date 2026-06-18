package com.example.mojaparafia

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity // 🔥 WRACAMY DO COMPONENT ACTIVITY
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.example.mojaparafia.billing.BillingManager
import com.example.mojaparafia.billing.SubscriptionManager
import com.example.mojaparafia.viewmodel.ParishListViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import mivs.mojaparafia.util.ReminderManager
import java.util.Calendar

// 🔥 POPRAWKA: Czyste ComponentActivity, idealne dla Compose KMP
class MainActivity : ComponentActivity(), BillingManager.BillingManagerListener {

    private val pushAction = MutableStateFlow<String?>(null)
    private val pushParishId = MutableStateFlow<String?>(null)
    private var billingManager: BillingManager? = null
    private lateinit var viewModel: ParishListViewModel
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var appUpdateManager: AppUpdateManager

    private lateinit var prefs: SharedPreferences
    protected val analytics: FirebaseAnalytics by lazy { Firebase.analytics }
    private var pendingLocationAction: (() -> Unit)? = null

    private val welcomeLocationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        prefs.edit { putBoolean("location_permission_shown", true) }
        runPermissionChain()
    }

    private val welcomeNotificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        prefs.edit { putBoolean("notif_permission_shown", true) }
        runPermissionChain()
    }

    private val welcomeAlarmLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
        prefs.edit { putBoolean("alarm_permission_shown", true) }
        runPermissionChain()
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            pendingLocationAction?.invoke()
        } else {
            Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_LONG).show()
        }
        pendingLocationAction = null
    }

    private val installStateUpdatedListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(applicationContext, "Aktualizacja pobrana. Trwa restartowanie aplikacji...", Toast.LENGTH_LONG).show()
            appUpdateManager.completeUpdate()
        }
    }

    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val kmpSettings = com.russhwolf.settings.Settings()
        val themeMode = kmpSettings.getInt("app_theme", 0)

        val nightMode = when (themeMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> {
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH)
                val time = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0

                val sunTimes = arrayOf(
                    8.0  to 15.75, 7.25 to 16.75, 6.25 to 17.75, 6.0  to 19.75,
                    5.0  to 20.75, 4.25 to 21.5,  4.75 to 21.25, 5.5  to 20.25,
                    6.5  to 19.0,  7.25 to 17.75, 7.25 to 15.75, 8.0  to 15.25
                )
                val (sunrise, sunset) = sunTimes[month]

                if (time < sunrise || time >= sunset) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            }
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)

        AndroidAppContext.initialize(applicationContext)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        viewModel = ViewModelProvider(this)[ParishListViewModel::class.java]
        val reminderManager = ReminderManager(this)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateUpdatedListener)
        FirebaseApp.initializeApp(this)

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val savedHomeId = kmpSettings.getStringOrNull("home_parish_id")
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        viewModel.initFromPlatform(deviceIdStr = androidId, savedHomeParishId = savedHomeId)

        val isFirstRun = kmpSettings.getBoolean("is_first_run", true)
        if (!isFirstRun) {
            viewModel.syncParishes()
            viewModel.fetchNews()
            scheduleNightlySync()
            checkForAppUpdates()
        }

        handleNotificationAction(intent)

        billingManager = SubscriptionManager.getInstance(this).billingManager
        billingManager?.setListener(this)

        setContent {
            val locationAction by viewModel.locationRequest.collectAsState()

            val isPremiumAndroid by billingManager!!.isPremium.observeAsState(false)
            LaunchedEffect(isPremiumAndroid) {
                viewModel.updatePremiumStatus(isPremiumAndroid)
            }

            val parishes by viewModel.allParishes.collectAsState(emptyList())

            val favoriteParishes by remember(parishes) {
                derivedStateOf { parishes.filter { it.isFavorite } }
            }
            LaunchedEffect(favoriteParishes) {
                favoriteParishes.forEach { parish ->
                    val topic = "parish_${parish.id}"
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                }
            }

            LaunchedEffect(Unit) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        viewModel.saveFcmToken(token) // Nasza wczorajsza metoda z ViewModelu!
                    }
                }
            }

            val action by pushAction.collectAsState()
            val parishId by pushParishId.collectAsState()

            LaunchedEffect(locationAction) {
                locationAction?.let { action ->
                    try {
                        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                viewModel.processUserLocation(location.latitude, location.longitude, action)
                            } else {
                                Toast.makeText(this@MainActivity, "Włącz GPS, by pobrać lokalizację", Toast.LENGTH_SHORT).show()
                            }
                            viewModel.onLocationRequestHandled()
                        }
                    } catch (e: SecurityException) {
                        Toast.makeText(this@MainActivity, "Brak uprawnień do lokalizacji", Toast.LENGTH_SHORT).show()
                        viewModel.onLocationRequestHandled()
                    }
                }
            }

            val productDetails by billingManager!!.productDetails.observeAsState(null)
            val monthlyOffer = productDetails?.subscriptionOfferDetails?.find { it.basePlanId == BillingManager.PLAN_MONTHLY }
            val monthlyPrice = monthlyOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

            val yearlyOffer = productDetails?.subscriptionOfferDetails?.find { it.basePlanId == BillingManager.PLAN_YEARLY }
            val yearlyPrice = yearlyOffer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

            App(
                viewModel = viewModel,
                pushAction = action,
                pushParishId = parishId,
                onPushHandled = {
                    pushAction.value = null
                    pushParishId.value = null
                },

                reminderScheduler = reminderManager,
                showToast = { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                },
                onRequestPlatformPermissions = { runPermissionChain() },

                onOpenPrivacyPolicy = {
                    val browserIntent = Intent(Intent.ACTION_VIEW, "https://misiek198910.github.io/mojaparafia-privacy/indexPL.html".toUri())
                    startActivity(browserIntent)
                },
                onOpenSystemSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                },
                onRestartAppRequired = { targetLanguage ->
                    if (targetLanguage != null) {
                        // Twardy restart procesu wyłącznie przy zmianie języka
                        val localeList = androidx.core.os.LocaleListCompat.forLanguageTags(targetLanguage)
                        androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(localeList)

                        val locale = java.util.Locale(targetLanguage)
                        java.util.Locale.setDefault(locale)
                        val config = resources.configuration
                        config.setLocale(locale)
                        createConfigurationContext(config)

                        val intent = Intent(this@MainActivity, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(intent)
                        Runtime.getRuntime().exit(0)
                    } else {
                        val currentSettings = com.russhwolf.settings.Settings()
                        val newThemeMode = currentSettings.getInt("app_theme", 0)

                        val newNightMode = when (newThemeMode) {
                            1 -> AppCompatDelegate.MODE_NIGHT_NO
                            2 -> AppCompatDelegate.MODE_NIGHT_YES
                            else -> {
                                val calendar = Calendar.getInstance()
                                val month = calendar.get(Calendar.MONTH)
                                val time = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
                                val sunTimes = arrayOf(
                                    8.0 to 15.75, 7.25 to 16.75, 6.25 to 17.75, 6.0 to 19.75,
                                    5.0 to 20.75, 4.25 to 21.5, 4.75 to 21.25, 5.5 to 20.25,
                                    6.5 to 19.0, 7.25 to 17.75, 7.25 to 15.75, 8.0 to 15.25
                                )
                                val (sunrise, sunset) = sunTimes[month]
                                if (time < sunrise || time >= sunset) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            }
                        }
                        AppCompatDelegate.setDefaultNightMode(newNightMode)
                    }
                },
                monthlyPriceStr = monthlyPrice,
                yearlyPriceStr = yearlyPrice,
                onBuyMonthlyClick = {
                    productDetails?.let { billingManager!!.launchPurchaseFlow(this@MainActivity, it, BillingManager.PLAN_MONTHLY) }
                },
                onBuyYearlyClick = {
                    productDetails?.let { billingManager!!.launchPurchaseFlow(this@MainActivity, it, BillingManager.PLAN_YEARLY) }
                },
                onManageSubscriptionsClick = {
                    val url = "https://play.google.com/store/account/subscriptions?package=$packageName"
                    startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                },
                onRestorePurchasesClick = {
                    billingManager?.queryPurchasesAsync()
                    Toast.makeText(this@MainActivity, "Przywracanie zakupów...", Toast.LENGTH_SHORT).show()
                },
                isIos = false // Jesteśmy na Androidzie, więc pokazujemy przyciski i ceny
            )
        }
    }

    override fun onPurchaseAcknowledged() {
        runOnUiThread {
            Toast.makeText(this, "Subskrypcja aktywowana!", Toast.LENGTH_LONG).show()
            viewModel.fetchUserStats() // Odświeżamy premium w aplikacji natychmiast
        }
    }

    override fun onPurchaseError(error: String?) {
        runOnUiThread {
            Toast.makeText(this, "Błąd zakupu: $error", Toast.LENGTH_LONG).show()
        }
    }

    private fun runPermissionChain() {
        when {
            !prefs.getBoolean("ad_consent_given", false) -> {
                val consentInformation = UserMessagingPlatform.getConsentInformation(this)
                consentInformation.requestConsentInfoUpdate(this, ConsentRequestParameters.Builder().build(), {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { _ ->
                        prefs.edit { putBoolean("ad_consent_given", true) }
                        runPermissionChain()
                    }
                }, {
                    prefs.edit { putBoolean("ad_consent_given", true) }
                    runPermissionChain()
                })
            }
            !prefs.getBoolean("location_permission_shown", false) -> {
                welcomeLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            !prefs.getBoolean("notif_permission_shown", false) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    welcomeNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    prefs.edit { putBoolean("notif_permission_shown", true) }
                    runPermissionChain()
                }
            }
            !prefs.getBoolean("alarm_permission_shown", false) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                        welcomeAlarmLauncher.launch(intent)
                        return
                    }
                }
                prefs.edit { putBoolean("alarm_permission_shown", true) }
                runPermissionChain()
            }
            else -> {
                viewModel.performInitialSyncAndFinish(
                    onError = {
                        Toast.makeText(this@MainActivity, "Błąd pobierania bazy. Sprawdź połączenie i spróbuj ponownie.", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationAction(intent)
        val targetLat = intent.getDoubleExtra("TARGET_LAT", 0.0)
        val targetLon = intent.getDoubleExtra("TARGET_LON", 0.0)
        if (targetLat != 0.0) {
            viewModel.focusMapOn(targetLat, targetLon)
        }
    }

    private fun handleNotificationAction(intent: Intent?) {
        intent?.extras?.let { bundle ->
            val action = bundle.getString("action") ?: bundle.get("action")?.toString()
            val parishId = bundle.getString("parish_id") ?: bundle.get("parish_id")?.toString()

            if (action != null) pushAction.value = action
            if (parishId != null) pushParishId.value = parishId
        }
    }

    override fun onResume() {
        super.onResume()
        val isFirstRun = !prefs.getBoolean("data_sync_completed", false)
        if (!isFirstRun) {
            viewModel.syncParishes()
            viewModel.fetchUserStats()
        }
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, this@MainActivity::class.simpleName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, this@MainActivity::class.simpleName)
        }
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateUpdatedListener)
    }
    private fun checkForAppUpdates() { }
    fun askForAppReview() { }
    private fun scheduleNightlySync() { }
}