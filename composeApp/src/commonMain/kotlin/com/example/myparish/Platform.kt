package com.example.myparish

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform