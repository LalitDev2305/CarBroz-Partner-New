package com.carbroz.partner.composition

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

/**
 * Canonical application composition module.
 *
 * Each subsystem continues to own its contracts and implementations; this module
 * only assembles those dependencies at the outer application boundary. New
 * subsystem modules are included here as their owning architecture phases land.
 */
val carBrozApplicationModule = module {
    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }

    single { StartupCoordinator(tasks = emptyList()) }
    single<ApplicationRuntime> { DefaultApplicationRuntime(startupCoordinator = get()) }
}

/**
 * Starts the single process-wide Koin container used by all platform hosts.
 *
 * Initialization is idempotent because Android activities and other host entry
 * points may be recreated while the process remains alive. Definition override
 * is disabled so duplicate ownership fails instead of silently replacing a
 * canonical dependency.
 *
 * Koin itself remains an implementation detail of the composition boundary;
 * platform hosts initialize the graph but do not receive or depend on [org.koin.core.Koin].
 */
fun initializeCarBrozDependencyInjection() {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        allowOverride(false)
        modules(carBrozApplicationModule)
    }
}
