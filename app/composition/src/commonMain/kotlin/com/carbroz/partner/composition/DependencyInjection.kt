package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.DefaultAppLifecycle
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.DefaultApplicationRuntime
import com.carbroz.runtime.application.startup.StartupCoordinator
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/** Canonical application composition module for one validated process configuration. */
fun carBrozApplicationModule(configuration: AppConfiguration) = module {
    single { configuration }
    single<ConfigurationProvider> { ConfigurationProvider { get<AppConfiguration>() } }

    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }

    single { StartupCoordinator(tasks = emptyList()) }
    single<ApplicationRuntime> { DefaultApplicationRuntime(startupCoordinator = get()) }
}

/**
 * Starts the single process-wide Koin container used by all platform hosts.
 *
 * The supplied [configuration] has already crossed the mandatory configuration-validation boundary.
 * Initialization is process-idempotent; subsequent host recreation cannot replace the canonical graph.
 */
fun initializeCarBrozDependencyInjection(configuration: AppConfiguration) {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        allowOverride(false)
        modules(carBrozApplicationModule(configuration))
    }
}
