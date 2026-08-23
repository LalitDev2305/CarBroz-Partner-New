package com.carbroz.partner.composition

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * Native iOS entry bridge into the shared Compose application.
 *
 * The Swift application host owns the iOS process/lifecycle while this factory
 * supplies the shared Compose root. Platform-specific services must be composed
 * through explicit iOS adapters rather than being implemented in SwiftUI views.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    CarBrozApp()
}
