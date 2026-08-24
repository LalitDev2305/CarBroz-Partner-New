package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseProvider
import com.carbroz.data.database.DatabaseHealthCheck
import com.carbroz.data.database.RoomDatabaseHealthCheck
import com.carbroz.data.network.ExecutorNetworkDataSource
import com.carbroz.data.network.InMemoryNetworkResponseCache
import com.carbroz.data.network.KtorNetworkTransport
import com.carbroz.data.network.NetworkAuthorizationProvider
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkEnvironmentProvider
import com.carbroz.data.network.NetworkExecutor
import com.carbroz.data.network.NetworkResponseCache
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.SessionNetworkAuthorizationProvider
import com.carbroz.data.network.createKtorNetworkTransport
import com.carbroz.data.preferences.PreferenceStore
import com.carbroz.data.preferences.PreferenceStoreProvider
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.DefaultAppLifecycle
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
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

/** Canonical application composition module for validated configuration and platform infrastructure. */
fun carBrozApplicationModule(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
    databaseProvider: CarBrozDatabaseProvider,
    preferenceStoreProvider: PreferenceStoreProvider,
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

    single<CarBrozDatabaseProvider> { databaseProvider }
    single<CarBrozDatabase> { get<CarBrozDatabaseProvider>().get() }
    single<DatabaseHealthCheck> { RoomDatabaseHealthCheck(database = get()) }

    single<PreferenceStoreProvider> { preferenceStoreProvider }
    single<PreferenceStore> { get<PreferenceStoreProvider>().get() }

    single { NetworkEnvironmentProvider(configurationProvider = get()) }
    single<NetworkEnvironment> { get<NetworkEnvironmentProvider>().get() }
    single { createKtorNetworkTransport() }
    single<NetworkTransport> { get<KtorNetworkTransport>() }
    single<NetworkAuthorizationProvider> { SessionNetworkAuthorizationProvider(sessionProvider = get()) }
    single<NetworkResponseCache> { InMemoryNetworkResponseCache() }
    single {
        NetworkExecutor(
            environment = get(),
            transport = get(),
            authorizationProvider = get(),
            responseCache = get(),
            clock = get(),
        )
    }
    single<NetworkDataSource> { ExecutorNetworkDataSource(executor = get()) }
    single { NetworkActionExecutor(dataSource = get()) }

    single { NavigationStore(NavigationState(listOf(AppShellDestination))) }

    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }

    single { StartupCoordinator(tasks = listOf(get<SessionRestoreStartupTask>())) }
    single<ApplicationRuntime> { DefaultApplicationRuntime(startupCoordinator = get()) }
}

/** Starts the single process-wide dependency graph after configuration/storage validation. */
internal fun initializeCarBrozDependencyInjection(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
    databaseProvider: CarBrozDatabaseProvider,
    preferenceStoreProvider: PreferenceStoreProvider,
) {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        allowOverride(false)
        modules(carBrozApplicationModule(configuration, secureStorage, databaseProvider, preferenceStoreProvider))
    }
}
