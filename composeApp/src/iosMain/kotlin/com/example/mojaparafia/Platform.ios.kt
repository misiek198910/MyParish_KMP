package com.example.mojaparafia

import platform.UIKit.UIDevice

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalWindowInfo
import org.jetbrains.compose.resources.DrawableResource
import platform.UIKit.*
import platform.Foundation.*
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.dispatch_after
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

actual fun showPlatformToast(message: String) {
    // iOS nie ma natywnych Toastów. Tworzymy lekki Alert i zamykamy go po 1.5 sekundy.
    val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)

    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController

    rootViewController?.presentViewController(alert, animated = true, completion = null)

    // Zamykamy po czasie (odpowiednik LENGTH_SHORT)
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (1.5 * NSEC_PER_SEC.toDouble()).toLong()), dispatch_get_main_queue()) {
        alert.dismissViewControllerAnimated(true, null)
    }
}

actual fun navigateToMap(parishId: String, lat: Double, lon: Double) {
    // 🔥 Wymuszamy typ Map<Any?, *> aby Objective-C i NSNotificationCenter były zadowolone
    val userInfo: Map<Any?, *> = mapOf(
        "parishId" to parishId,
        "lat" to lat,
        "lon" to lon
    )
    NSNotificationCenter.defaultCenter.postNotificationName("NavigateToMapCommand", null, userInfo)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun isLandscapeOrientation(): Boolean {
    // Metoda wieloplatformowa: sprawdzamy geometrię okna w KMP
    val windowInfo = LocalWindowInfo.current
    return windowInfo.containerSize.width > windowInfo.containerSize.height
}

@Composable
actual fun AdBannerView(modifier: Modifier) {

    UIKitView(
        factory = { UIView() },
        modifier = modifier
    )
}

actual fun generateAndShareIntentionImage(text: String, backgroundBitmap: ImageBitmap) {

    val activityItems = listOf(text)
    val activityController = UIActivityViewController(activityItems, null)

    val window = UIApplication.sharedApplication.keyWindow
    val rootViewController = window?.rootViewController

    activityController.popoverPresentationController?.sourceView = rootViewController?.view

    rootViewController?.presentViewController(activityController, animated = true, completion = null)
}

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()