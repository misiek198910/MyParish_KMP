import SwiftUI
import ComposeApp
import GoogleMobileAds

@main
struct iOSApp: App {
    
    init() {
        ParishMapBridge.shared.globalSwiftMapFactory = NativeMapFactoryImpl()

        let adBannerID = Bundle.main.object(forInfoDictionaryKey: "AD_BANNER_ID") as? String ?? ""
        let adInlineID = Bundle.main.object(forInfoDictionaryKey: "AD_BANNER_INLINE_ID") as? String ?? ""
        
        // 1. Start AdMob
        MobileAds.shared.requestConfiguration.testDeviceIdentifiers = [ "kGADSimulatorID" ]
        MobileAds.shared.start(completionHandler: nil)

        // 2. Dolny baner
        AdBanner_iosKt.createIosAdBannerView = {
            let banner = BannerView(adSize: AdSizeBanner)
            banner.adUnitID = adBannerID

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first,
               let rootVC = window.rootViewController {
                banner.rootViewController = rootVC
            }

            banner.load(Request())
            banner.backgroundColor = UIColor.white
            return banner
        }
        
        // 3. Baner Inline
        InlineAdBanner_iosKt.createIosInlineAdBannerView = {
            let width = UIScreen.main.bounds.width
            let adSize = inlineAdaptiveBanner(width: width, maxHeight: 250)
            
            let banner = BannerView(adSize: adSize)
            banner.adUnitID = adInlineID

            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
               let window = windowScene.windows.first,
               let rootVC = window.rootViewController {
                banner.rootViewController = rootVC
            }

            banner.load(Request())
            return banner
        }
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
