package com.carbroz.partner.app.composition

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.carbroz.partner.app.composition.config.AppConfig
import com.carbroz.partner.infrastructure.persistence.session.IosSessionCredentialStoreFactory
import platform.UIKit.UIViewController

/**
 * iOS Platform Host entry point returning a [UIViewController] embedding [CarBrozPartnerRoot].
 */
public fun MainViewController(
    config: AppConfig = AppConfig.production()
): UIViewController = ComposeUIViewController {
    val credentialStore = remember { IosSessionCredentialStoreFactory.create() }
    CarBrozPartnerRoot(credentialStore = credentialStore, config = config)
}
