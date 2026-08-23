package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.DefaultAppLifecycle
import com.carbroz.foundation.security.SecureStorage
import com.carbroz.foundation.session.JsonSessionSnapshotCodec
import com.carbroz.foundation.session.SecureSessionPersistence
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionSnapshotCodec
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.TokenExpiryPolicy
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.DefaultApplicationRuntime
import com.carbroz.runtime.application.startup.StartupCoordinator
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

/** Canonical application composition module for validated configuration and platform secure storage. */
fun carBrozApplicationModule(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
) = module {
    single { configuration }
    single<ConfigurationProvider> { ConfigurationProvider { get<AppConfiguration>() } }

    single<Clock> { SystemClock }
    single<SecureStorage> { secureStorage }
    single<SessionSnapshotCodec> { JsonSessionSnapshotCodec() }
    single<SessionPersistence> {
        SecureSessionPersistence(
            secureStorage = get(),
            codec = get(),
        )
    }
    single { SessionStore(persistence = get()) }
    single<SessionProvider> { get<SessionStore>() }
    single { TokenExpiryPolicy(clock = get()) }
    single { SessionRestoreStartupTask(sessionStore = get()) }

    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }

    single { StartupCoordinator(tasks = listOf(get<SessionRestoreStartupTask>())) }
    single<ApplicationRuntime> { DefaultApplicationRuntime(startupCoordinator = get()) }
}

/** Starts the single process-wide dependency graph after configuration/storage validation. */
internal fun initializeCarBrozDependencyInjection(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
) {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        allowOverride(false)
        modules(carBrozApplicationModule(configuration, secureStorage))
    }
}
