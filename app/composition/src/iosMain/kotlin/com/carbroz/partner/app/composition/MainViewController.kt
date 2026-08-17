package com.carbroz.partner.app.composition

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Main UIViewController builder for iOS platform host integration.
 *
 * Exposes the multiplatform Compose entry point [CarBrozPartnerRoot] to Apple UIKit environment.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CarBrozPartnerRoot()
}
