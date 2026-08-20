package com.carbroz.partner.app.composition

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.infrastructure.persistence.session.IosSessionCredentialPersistenceFactory
import platform.UIKit.UIViewController

public fun MainViewController(
    config: AppConfig = AppConfig.production()
): UIViewController = ComposeUIViewController {
    val credentialPersistence = remember { IosSessionCredentialPersistenceFactory.create() }
    CarBrozPartnerRoot(
        credentialPersistence = credentialPersistence,
        config = config
    )
}
