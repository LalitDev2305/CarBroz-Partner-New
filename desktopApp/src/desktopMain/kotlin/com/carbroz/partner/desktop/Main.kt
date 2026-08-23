package com.carbroz.partner.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.carbroz.partner.composition.AppLifecycleBridge
import com.carbroz.partner.composition.CarBrozApp
import com.carbroz.partner.composition.initializeCarBrozDependencyInjection

/** Desktop host entry point. */
fun main() {
    initializeCarBrozDependencyInjection()
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
