package com.carbroz.partner.ios

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.carbroz.partner.app.composition.CarBrozPartnerRoot
import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.infrastructure.persistence.session.IosSessionCredentialPersistenceFactory
import platform.UIKit.UIViewController

/**
 * iOS Platform Host Controller Entry Point.
 *
 * Constructs iOS platform-specific persistence dependencies at the host boundary
 * and supplies them to shared [CarBrozPartnerRoot].
 */
public fun MainViewController(
    config: AppConfig = AppConfig.production()
): UIViewController = ComposeUIViewController {
    val credentialPersistence = remember { IosSessionCredentialPersistenceFactory.create() }
    CarBrozPartnerRoot(
        credentialPersistence = credentialPersistence,
        config = config
    )
}
