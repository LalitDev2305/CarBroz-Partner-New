package com.carbroz.partner.app.composition

import com.carbroz.partner.app.composition.bootstrap.createIosAppController
import com.carbroz.partner.app.composition.config.AppConfig
import platform.UIKit.UIViewController

/**
 * iOS Platform Host entry point returning a [UIViewController] embedding [CarBrozPartnerRoot].
 */
public fun MainViewController(
    config: AppConfig = AppConfig.production()
): UIViewController {
    return createIosAppController(config)
}
