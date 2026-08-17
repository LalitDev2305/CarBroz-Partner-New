package com.carbroz.partner

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Creates the iOS [UIViewController] hosting the shared Compose UI framework.
 *
 * This entry point is exported as a static framework binary for consumption by Swift
 * in `iosApp` (`iOSApp.swift`), bridging Apple UIKit view controller lifecycle to common Compose.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CarBrozPartnerRoot()
}

