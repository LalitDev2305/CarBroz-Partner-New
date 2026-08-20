package com.carbroz.partner.app.composition.bootstrap

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.carbroz.partner.app.composition.CarBrozPartnerRoot
import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.infrastructure.persistence.session.IosSessionCredentialPersistenceFactory
import platform.UIKit.UIViewController

/**
 * iOS Platform Application Bootstrap constructing iOS platform-specific dependencies and root UIViewController.
 */
public fun createIosAppController(
    config: AppConfig = AppConfig.production()
): UIViewController = ComposeUIViewController {
    val credentialPersistence = remember { IosSessionCredentialPersistenceFactory.create() }
    CarBrozPartnerRoot(
        credentialPersistence = credentialPersistence,
        config = config
    )
}
