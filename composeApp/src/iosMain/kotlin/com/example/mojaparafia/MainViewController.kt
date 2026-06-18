package com.example.mojaparafia

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.mojaparafia.util.Reminder
import com.example.mojaparafia.util.ReminderScheduler
import com.example.mojaparafia.viewmodel.ParishListViewModel
import com.russhwolf.settings.Settings
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceStyle
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import platform.posix.exit
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

class IosReminderScheduler : ReminderScheduler {
    override fun scheduleReminder(reminder: Reminder) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound
        ) { granted, error ->
            if (granted && error == null) {
                val content = UNMutableNotificationContent().apply {
                    setTitle(reminder.parishName)
                    setBody("Msza Święta rozpocznie się o ${reminder.massTime}")
                    setSound(UNNotificationSound.defaultSound())
                }
                val dateComponents = NSDateComponents().apply {
                    setYear(reminder.reminderDateTime.year.toLong())
                    setMonth(reminder.reminderDateTime.monthNumber.toLong())
                    setDay(reminder.reminderDateTime.dayOfMonth.toLong())
                    setHour(reminder.reminderDateTime.hour.toLong())
                    setMinute(reminder.reminderDateTime.minute.toLong())
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(dateComponents, false)
                val request = UNNotificationRequest.requestWithIdentifier(reminder.notificationId.toString(), content, trigger)
                center.addNotificationRequest(request) { err ->
                    if (err != null) println("Błąd dodawania powiadomienia iOS: ${err.localizedDescription}")
                }
            }
        }
    }
    override fun cancelReminder(notificationId: Int) {
        UNUserNotificationCenter.currentNotificationCenter().removePendingNotificationRequestsWithIdentifiers(listOf(notificationId.toString()))
    }
    override fun getActiveReminders(): List<Reminder> = emptyList()
}

class SimpleLocationManager(
    private val onLocationUpdate: (Double, Double) -> Unit,
    private val onLocationError: (String) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {
    private val locationManager = CLLocationManager()
    init {
        locationManager.delegate = this
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }
    fun requestLocation() {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> locationManager.startUpdatingLocation()
            else -> onLocationError("Brak uprawnień. Otwórz Ustawienia iPhone'a, aby nadać zgodę na GPS.")
        }
    }
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
            manager.startUpdatingLocation()
        } else if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
            onLocationError("Uprawnienia do GPS zostały odrzucone.")
        }
    }
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        location?.coordinate?.let { coord ->
            onLocationUpdate(coord.useContents { latitude }, coord.useContents { longitude })
            manager.stopUpdatingLocation()
        } ?: onLocationError("System iOS zwrócił pustą lokalizację.")
    }
    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onLocationError(didFailWithError.localizedDescription)
    }
}

@Suppress("FunctionName", "unused")
fun MainViewController() = ComposeUIViewController {
    val iosViewModel = remember { ParishListViewModel() }

    val iosDeviceId = UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown_ios_device"
    val settings = Settings()
    val savedHomeParishId = settings.getStringOrNull("home_parish_id")

    LaunchedEffect(Unit) {
        val themeMode = settings.getInt("app_theme", 0)
        val window = UIApplication.sharedApplication.keyWindow

        when (themeMode) {
            1 -> window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
            2 -> window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
            else -> {
                val calendar = NSCalendar.currentCalendar
                val date = NSDate()
                val month = calendar.component(NSCalendarUnitMonth, date).toInt() - 1
                val time = calendar.component(NSCalendarUnitHour, date).toInt() + calendar.component(NSCalendarUnitMinute, date).toInt() / 60.0

                val sunTimes = arrayOf(
                    8.0  to 15.75, 7.25 to 16.75, 6.25 to 17.75, 6.0  to 19.75,
                    5.0  to 20.75, 4.25 to 21.5,  4.75 to 21.25, 5.5  to 20.25,
                    6.5  to 19.0,  7.25 to 17.75, 7.25 to 15.75, 8.0  to 15.25
                )
                val (sunrise, sunset) = sunTimes[month]

                window?.overrideUserInterfaceStyle = if (time < sunrise || time >= sunset) {
                    UIUserInterfaceStyle.UIUserInterfaceStyleDark
                } else {
                    UIUserInterfaceStyle.UIUserInterfaceStyleLight
                }
            }
        }

        iosViewModel.initFromPlatform(
            deviceIdStr = iosDeviceId,
            savedHomeParishId = savedHomeParishId
        )
    }

    val locationAction by iosViewModel.locationRequest.collectAsState()

    val locationManager = remember {
        SimpleLocationManager(
            onLocationUpdate = { lat, lon ->
                val currentAction = iosViewModel.locationRequest.value
                if (currentAction != null) {
                    iosViewModel.processUserLocation(lat, lon, currentAction)
                    iosViewModel.onLocationRequestHandled()
                }
            },
            onLocationError = { errorMsg ->
                iosViewModel.onLocationRequestHandled()
                val alert = UIAlertController.alertControllerWithTitle("Błąd GPS (iOS)", "Szczegóły: $errorMsg\n\n[Wskazówka] Jeśli testujesz na symulatorze Xcode, upewnij się, że w górnym pasku menu włączyłeś: Features -> Location -> np. 'Apple'.", platform.UIKit.UIAlertControllerStyleAlert)
                alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
                UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(alert, true, null)
            }
        )
    }

    val iosReminderScheduler = remember { IosReminderScheduler() }

    LaunchedEffect(locationAction) {
        locationAction?.let { locationManager.requestLocation() }
    }

    App(
        viewModel = iosViewModel,
        reminderScheduler = iosReminderScheduler,
        showToast = { message ->
            val alert = UIAlertController.alertControllerWithTitle(null, message, platform.UIKit.UIAlertControllerStyleAlert)
            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(alert, true, null)
        },
        onRequestPlatformPermissions = {
            // 1. Pytamy o powiadomienia (To wywoła systemowy popup iOS)
            val center = UNUserNotificationCenter.currentNotificationCenter()
            center.requestAuthorizationWithOptions(
                UNAuthorizationOptionAlert or UNAuthorizationOptionSound
            ) { _, _ ->

                dispatch_async(dispatch_get_main_queue()) {
                    locationManager.requestLocation()
                    iosViewModel.performInitialSyncAndFinish(
                        onError = {
                            val alert = UIAlertController.alertControllerWithTitle(
                                "Błąd",
                                "Nie udało się pobrać danych. Sprawdź połączenie z internetem.",
                                platform.UIKit.UIAlertControllerStyleAlert
                            )
                            alert.addAction(UIAlertAction.actionWithTitle("OK", UIAlertActionStyleDefault, null))
                            UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(alert, true, null)
                        }
                    )
                }
            }
        },

        onOpenPrivacyPolicy = {
            val url = NSURL(string = "https://misiek198910.github.io/mojaparafia-privacy/indexPL.html")
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            }
        },
        onOpenSystemSettings = {
            val url = NSURL(string = UIApplicationOpenSettingsURLString)
            if (url != null && UIApplication.sharedApplication.canOpenURL(url)) {
                UIApplication.sharedApplication.openURL(url, emptyMap<Any?, Any>(), null)
            }
        },
        onRestartAppRequired = { targetLanguage ->
            if (targetLanguage != null) {
                // Twardy restart procesu wyłącznie przy zmianie języka
                val userDefaults = platform.Foundation.NSUserDefaults.standardUserDefaults
                val languagesArray = listOf(targetLanguage)
                userDefaults.setObject(languagesArray, "AppleLanguages")
                userDefaults.synchronize()
                exit(0)
            } else {
                // 🔥 POPRAWKA: Zmiana motywu na iOS realizowana dynamicznie na aktywnym oknie
                val themeMode = Settings().getInt("app_theme", 0)
                val window = UIApplication.sharedApplication.keyWindow

                when (themeMode) {
                    1 -> window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleLight
                    2 -> window?.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
                    else -> {
                        val calendar = NSCalendar.currentCalendar
                        val date = NSDate()
                        val month = calendar.component(NSCalendarUnitMonth, date).toInt() - 1
                        val time = calendar.component(NSCalendarUnitHour, date).toInt() + calendar.component(NSCalendarUnitMinute, date).toInt() / 60.0

                        val sunTimes = arrayOf(
                            8.0  to 15.75, 7.25 to 16.75, 6.25 to 17.75, 6.0  to 19.75,
                            5.0  to 20.75, 4.25 to 21.5,  4.75 to 21.25, 5.5  to 20.25,
                            6.5  to 19.0,  7.25 to 17.75, 7.25 to 15.75, 8.0  to 15.25
                        )
                        val (sunrise, sunset) = sunTimes[month]

                        window?.overrideUserInterfaceStyle = if (time < sunrise || time >= sunset) {
                            UIUserInterfaceStyle.UIUserInterfaceStyleDark
                        } else {
                            UIUserInterfaceStyle.UIUserInterfaceStyleLight
                        }
                    }
                }
            }
        },
        monthlyPriceStr = null,
        yearlyPriceStr = null,
        onBuyMonthlyClick = { },
        onBuyYearlyClick = { },
        onManageSubscriptionsClick = { },
        onRestorePurchasesClick = { },
        isIos = true
    )
}