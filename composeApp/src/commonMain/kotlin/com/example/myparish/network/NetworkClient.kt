package com.example.myparish.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

val networkClient = HttpClient {
    // ContentNegotiation odpowiada za automatyczne parsowanie JSON na klasy Kotlinowe
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true // Ignoruj pola z API, których nie masz w modelu
            prettyPrint = true
            isLenient = true
        })
    }

    // Logging pozwoli Ci widzieć w Logcat (Android) i konsoli (iOS) co dokładnie wysyłasz i odbierasz
    install(Logging) {
        level = LogLevel.BODY
        logger = Logger.DEFAULT
    }
}