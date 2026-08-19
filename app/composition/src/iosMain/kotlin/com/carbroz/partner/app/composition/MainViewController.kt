package com.carbroz.partner.app.composition

import androidx.compose.ui.window.ComposeUIViewController
import com.carbroz.partner.app.composition.config.AppConfig
import platform.UIKit.UIViewController

/**
 * iOS Platform Host entry point returning a [UIViewController] embedding [CarBrozPartnerRoot].
 */
public fun MainViewController(
    config: AppConfig = AppConfig.production()
): UIViewController = ComposeUIViewController {
    CarBrozPartnerRoot(config = config)
}
