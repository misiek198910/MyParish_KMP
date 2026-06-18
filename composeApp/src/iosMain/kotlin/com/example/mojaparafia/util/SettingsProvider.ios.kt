package com.example.mojaparafia.util

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

actual fun getSettings(): Settings {

    return KeychainSettings(service = "MyParishSecureStore")
}