import SwiftUI
import ComposeApp
import GoogleMobileAds
import FirebaseCore
import FirebaseMessaging
import UserNotifications
import UserMessagingPlatform

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

class ConsentManager {
    static let shared = ConsentManager()

    func gatherConsent(completion: @escaping () -> Void) {
        let parameters = RequestParameters()

        ConsentInformation.shared.requestConsentInfoUpdate(with: parameters) { error in
            if error != nil {
                completion()
                return
            }

            DispatchQueue.main.async {
                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                      let rootViewController = windowScene.windows.first?.rootViewController else {
                    completion()
                    return
                }

                ConsentForm.loadAndPresentIfRequired(from: rootViewController) { _ in
                    completion()
                }
            }
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

        let adBannerID = Bundle.main.object(forInfoDictionaryKey: "AD_BANNER_ID") as? String ?? ""
        let adInlineID = Bundle.main.object(forInfoDictionaryKey: "AD_BANNER_INLINE_ID") as? String ?? ""

        MobileAds.shared.requestConfiguration.testDeviceIdentifiers = [ "kGADSimulatorID" ]

        AdBanner_iosKt.createIosAdBannerView = {
            let banner = BannerView(adSize: AdSizeBanner)
            banner.adUnitID = adBannerID

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first,
               let rootVC = window.rootViewController {
                banner.rootViewController = rootVC
            }

            if ConsentInformation.shared.canRequestAds {
                banner.load(Request())
            }

            banner.backgroundColor = UIColor.white
            return banner
        }

        InlineAdBanner_iosKt.createIosInlineAdBannerView = {
            let width = UIScreen.main.bounds.width
            let banner = BannerView(adSize: inlineAdaptiveBanner(width: width, maxHeight: 250))
            banner.adUnitID = adInlineID

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first,
               let rootVC = window.rootViewController {
                banner.rootViewController = rootVC
            }

            if ConsentInformation.shared.canRequestAds {
                banner.load(Request())
            }

            return banner
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView(pushManager: pushManager)
                .onAppear {
                    ConsentManager.shared.gatherConsent {
                        if ConsentInformation.shared.canRequestAds {
                            MobileAds.shared.start(completionHandler: nil)
                        }
                    }
                }
        }
    }
}
