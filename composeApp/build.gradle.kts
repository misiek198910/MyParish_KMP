import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.googleServices)
}

room {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.compilations.getByName("main").compileTaskProvider.configure {
            compilerOptions {
                freeCompilerArgs.addAll(
                    listOf("-Xdisable-phases=Devirtualization,Inliner")
                )
            }
        }
    }


    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.kotlinx.datetime)
            implementation(libs.settings.core)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.settings.core)
            implementation(libs.settings.noarg)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
            implementation(libs.lifecycle.viewmodel)

        }

        androidMain.dependencies {

            implementation(libs.billing.client)
            implementation(libs.compose.livedata)
            implementation(libs.billing.ktx)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.settings.core)
            implementation(libs.gson)
            implementation(libs.play.services.maps)
            implementation(libs.play.services.location)
            implementation(libs.maps.compose)
            implementation(libs.maps.utils)
            implementation(libs.play.services.ads)
            implementation(libs.play.app.update)
            implementation(libs.play.app.update.ktx)
            implementation(libs.play.review)
            implementation(libs.play.review.ktx)
            implementation(libs.firebase.analytics.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.work.runtime)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.example.mojaparafia"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "mivs.mojaparafia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 52
        versionName = "1.2.33"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["mapsApiKey"] = ""

        val fbAppId = localProperties.getProperty("FACEBOOK_APP_ID", "")
        val fbClientToken = localProperties.getProperty("FACEBOOK_CLIENT_TOKEN", "")

        // Wstrzykiwanie kluczy FB do kodu i zasobów XML (np. dla AndroidManifest)
        buildConfigField("String", "FACEBOOK_APP_ID", "\"$fbAppId\"")
        buildConfigField("String", "FACEBOOK_CLIENT_TOKEN", "\"$fbClientToken\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("MYAPP_RELEASE_STORE_FILE", "brak-sciezki"))
            storePassword = localProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        getByName("debug") {
            val debugKey = localProperties.getProperty("MAPS_API_KEY_DEBUG") ?: "BRAK_KLUCZA_DEBUG"
            manifestPlaceholders["mapsApiKey"] = debugKey
            manifestPlaceholders["adMobAppId"] = "ca-app-pub-3940256099942544~3347511713"

            buildConfigField("String", "AD_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_START_UNIT_ID", "\"ca-app-pub-3940256099942544/9257395921\"")
            buildConfigField("String", "AD_BANNER_INLINE_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")

            val releaseMapsKey = localProperties.getProperty("MAPS_API_KEY_RELEASE") ?: ""
            val bannerId = localProperties.getProperty("AD_BANNER_ID") ?: ""
            val bannerInlineId = localProperties.getProperty("AD_BANNER_INLINE_ID") ?: ""
            val adStartId = localProperties.getProperty("AD_START_UNIT_ID") ?: ""
            val adMobAppId = localProperties.getProperty("AD_APP_ID") ?: ""
            val adRewardedId = localProperties.getProperty("AD_REWARDED_ID") ?: ""

            manifestPlaceholders["mapsApiKey"] = releaseMapsKey
            manifestPlaceholders["adMobAppId"] = adMobAppId

            buildConfigField("String", "AD_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "AD_BANNER_INLINE_ID", "\"$bannerInlineId\"")
            buildConfigField("String", "AD_START_UNIT_ID", "\"$adStartId\"")
            buildConfigField("String", "AD_REWARDED_ID", "\"$adRewardedId\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // Pamiętaj, że dla KMP często używamy 11 lub 17
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        // TO JEST KLUCZOWE: Pozwala na używanie buildConfigField w kodzie!
        buildConfig = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    ksp(libs.room.compiler)
}

compose {
    resources {
        publicResClass = true
    }
}

