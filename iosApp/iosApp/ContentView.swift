import UIKit
import SwiftUI
import ComposeApp

struct ContentView: UIViewControllerRepresentable {
    let pushManager: IosPushManager

    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController(pushManager: pushManager)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView_Previews: PreviewProvider {

    class PreviewPushManager: NSObject, IosPushManager {
        func subscribeToTopic(topic: String) {}
        func unsubscribeFromTopic(topic: String) {}
        func getFcmToken(onResult: @escaping (String) -> Void) {}
    }

    static var previews: some View {
        ContentView(pushManager: PreviewPushManager())
    }
}