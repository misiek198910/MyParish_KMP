import SwiftUI
import ComposeApp
import FirebaseCore
import FirebaseMessaging
import UserNotifications

class SwiftPushManager: NSObject, IosPushManager {
    func getFcmToken(onResult: @escaping (String) -> Void) {
        Messaging.messaging().token { token, error in
            if let t = token { onResult(t) }
        }
    }

    func subscribeToTopic(topic: String) {
        if Messaging.messaging().apnsToken != nil {
            Messaging.messaging().subscribe(toTopic: topic)
        } else {
            DispatchQueue.main.asyncAfter(deadline: .now() + 3.0) {
                self.subscribeToTopic(topic: topic)
            }
        }
    }

    func unsubscribeFromTopic(topic: String) {
        if Messaging.messaging().apnsToken != nil {
            Messaging.messaging().unsubscribe(fromTopic: topic)
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate, MessagingDelegate, UNUserNotificationCenterDelegate {

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        MainViewControllerKt.setIosPushData(action: userInfo["action"] as? String, parishId: userInfo["parish_id"] as? String)
        completionHandler()
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    let pushManager = SwiftPushManager()

    init() {
        ParishMapBridge.shared.globalSwiftMapFactory = NativeMapFactoryImpl()
    }

    var body: some Scene {
        WindowGroup {
            ContentView(pushManager: pushManager)
        }
    }
}