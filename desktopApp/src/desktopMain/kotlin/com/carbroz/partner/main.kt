package com.carbroz.partner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.carbroz.partner.app.composition.CarBrozPartnerRoot
import com.carbroz.partner.app.composition.config.AppConfig

/**
 * JVM Desktop Platform Host entry point.
 */
fun main() = application {
    val config = AppConfig(
        baseUrl = "https://api.carbroz.com"
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "CarBroz Partner"
    ) {
        CarBrozPartnerRoot(config = config)
    }
}
