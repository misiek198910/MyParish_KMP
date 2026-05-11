package com.example.myparish.util

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

actual fun getSettings(): Settings {

    return KeychainSettings(service = "MyParishSecureStore")
}