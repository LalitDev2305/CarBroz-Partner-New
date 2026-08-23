package com.carbroz.partner.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.carbroz.partner.composition.AppLifecycleBridge
import com.carbroz.partner.composition.CarBrozApp
import com.carbroz.partner.composition.initializeCarBrozDesktopApplication

/** Desktop host entry point. */
fun main() {
    val environment = systemSetting("carbroz.environment", "CARBROZ_ENVIRONMENT") ?: "development"
    val apiBaseUrl = systemSetting("carbroz.apiBaseUrl", "CARBROZ_API_BASE_URL")
        ?: defaultDesktopApiBaseUrl(environment)

    initializeCarBrozDesktopApplication(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        versionName = "1.0.0",
        versionCode = 1L,
        applicationId = "com.carbroz.partner.desktop",
    )
    AppLifecycleBridge.moveToForeground()

    application {
        Window(
            onCloseRequest = {
                AppLifecycleBridge.moveToBackground()
                exitApplication()
            },
            title = "CarBroz Partner",
        ) {
            CarBrozApp()
        }
    }
}

private fun systemSetting(property: String, environmentVariable: String): String? =
    System.getProperty(property)?.takeIf(String::isNotBlank)
        ?: System.getenv(environmentVariable)?.takeIf(String::isNotBlank)

private fun defaultDesktopApiBaseUrl(environment: String): String = when (environment.trim().lowercase()) {
    "development", "dev" -> "https://development.invalid"
    "staging", "stage" -> "https://staging.invalid"
    "production", "prod" -> "https://production.invalid"
    else -> error("Unsupported CarBroz environment: '$environment'.")
}
