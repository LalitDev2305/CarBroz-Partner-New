package com.carbroz.partner

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.carbroz.partner.app.composition.CarBrozPartnerRoot
import com.carbroz.partner.infrastructure.persistence.session.DesktopSessionCredentialStoreFactory

/**
 * JVM Desktop Platform Host entry point.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CarBroz Partner"
    ) {
        val credentialStore = remember { DesktopSessionCredentialStoreFactory.create() }
        CarBrozPartnerRoot(credentialStore = credentialStore)
    }
}
