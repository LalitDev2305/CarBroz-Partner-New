package com.carbroz.partner

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.carbroz.partner.app.composition.CarBrozPartnerRoot

/**
 * JVM Desktop Platform Host entry point.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CarBroz Partner"
    ) {
        CarBrozPartnerRoot()
    }
}
