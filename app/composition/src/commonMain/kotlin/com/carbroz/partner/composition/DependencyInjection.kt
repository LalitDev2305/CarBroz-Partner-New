package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseProvider
import com.carbroz.data.database.DatabaseHealthCheck
import com.carbroz.data.database.RoomDatabaseHealthCheck
import com.carbroz.data.network.ExecutorNetworkDataSource
import com.carbroz.data.network.InMemoryNetworkResponseCache
import com.carbroz.data.network.KtorNetworkTransport
import com.carbroz.data.network.NetworkAuthorizationProvider
import com.carbroz.data.network.NetworkConnectivityObserver
import com.carbroz.data.network.NetworkConnectivityProvider
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
import com.carbroz.data.realtime.KtorRealtimeTransport
import com.carbroz.data.realtime.RealtimeDeliveryGate
import com.carbroz.data.realtime.RealtimeStream
import com.carbroz.data.realtime.RealtimeTransport
import com.carbroz.data.realtime.createKtorRealtimeTransport
import com.carbroz.data.sync.DefaultSyncCoordinator
import com.carbroz.data.sync.KeepQueuedSyncConflictResolver
import com.carbroz.data.sync.OutboxStore
import com.carbroz.data.sync.RoomOutboxStore
import com.carbroz.data.sync.SyncConflictResolver
import com.carbroz.data.sync.SyncCoordinator
import com.carbroz.foundation.analytics.AnalyticsPolicy
import com.carbroz.foundation.analytics.AnalyticsTracker
import com.carbroz.foundation.capabilities.CapabilityProvider
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.lifecycle.DefaultAppLifecycle
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
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
import com.carbroz.platform.background.BackgroundScheduler
import com.carbroz.platform.background.BackgroundTaskHandler
import com.carbroz.platform.background.BackgroundTaskHandlerRegistry
import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.ContinuousExecutionController
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
    capabilityProviders: List<CapabilityProvider> = emptyList(),
    backgroundScheduler: BackgroundScheduler? = null,
    continuousExecutionController: ContinuousExecutionController? = null,
    backgroundTaskHandlers: List<BackgroundTaskHandler> = emptyList(),
) = module {
    single { configuration }
    single<ConfigurationProvider> { ConfigurationProvider { get<AppConfiguration>() } }

    single {
        Observability(
            policy = ObservabilityPolicy(
                minimumLogLevel = when (configuration.environment) {
                    AppEnvironment.Development -> LogLevel.DEBUG
                    AppEnvironment.Staging -> LogLevel.INFO
                    AppEnvironment.Production -> LogLevel.WARN
                },
                crashReportingEnabled = true,
                performanceMetricsEnabled = true,
            ),
        )
    }
    single {
        AnalyticsTracker(
            policy = AnalyticsPolicy(
                enabled = false,
                allowedEventNames = emptySet(),
            ),
        )
    }

    single<Clock> { SystemClock }
    single<SecureStorage> { secureStorage }
    single<SessionSnapshotCodec> { JsonSessionSnapshotCodec() }
    single<SessionPersistence> { SecureSessionPersistence(secureStorage = get(), codec = get()) }
    single { SessionStore(persistence = get()) }
    single<SessionProvider> { get<SessionStore>() }
    single { TokenExpiryPolicy(clock = get()) }
    single { SessionRestoreStartupTask(sessionStore = get()) }

    single<CarBrozDatabaseProvider> { databaseProvider }
    single<CarBrozDatabase> { get<CarBrozDatabaseProvider>().get() }
    single<DatabaseHealthCheck> { RoomDatabaseHealthCheck(database = get()) }

    single<PreferenceStoreProvider> { preferenceStoreProvider }
    single<PreferenceStore> { get<PreferenceStoreProvider>().get() }

    single<CapabilityRegistry> { createCapabilityRegistry(capabilityProviders) }
    single { CapabilityActionExecutor(registry = get()) }

    single { BackgroundTaskHandlerRegistry(backgroundTaskHandlers) }
    single { BackgroundTaskRunner(registry = get()) }
    if (backgroundScheduler != null) single<BackgroundScheduler> { backgroundScheduler }
    if (continuousExecutionController != null) single<ContinuousExecutionController> { continuousExecutionController }

    single { ApplicationConnectivity() }
    single<NetworkConnectivityObserver> { get<ApplicationConnectivity>() }
    single<NetworkConnectivityProvider> { get<ApplicationConnectivity>() }
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
            connectivityProvider = get(),
            responseCache = get(),
            clock = get(),
        )
    }
    single<NetworkDataSource> { ExecutorNetworkDataSource(executor = get()) }
    single { NetworkActionExecutor(dataSource = get()) }

    single { createKtorRealtimeTransport() }
    single<RealtimeTransport> { get<KtorRealtimeTransport>() }
    single { RealtimeStream(transport = get()) }
    single { RealtimeDeliveryGate() }

    single<OutboxStore> { RoomOutboxStore(database = get()) }
    single<SyncConflictResolver> { KeepQueuedSyncConflictResolver }
    single<SyncCoordinator> {
        DefaultSyncCoordinator(
            outbox = get(),
            network = get(),
            clock = get(),
            conflictResolver = get(),
        )
    }

    single { NavigationStore(NavigationState(listOf(AppShellDestination))) }

    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }
    single { SyncActivationCoordinator(lifecycle = get(), connectivity = get(), syncCoordinator = get()) }

    single { StartupCoordinator(tasks = listOf(get<SessionRestoreStartupTask>())) }
    single<ApplicationRuntime> { DefaultApplicationRuntime(startupCoordinator = get()) }
}

/** Starts the single process-wide dependency graph after configuration/storage validation. */
internal fun initializeCarBrozDependencyInjection(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
    databaseProvider: CarBrozDatabaseProvider,
    preferenceStoreProvider: PreferenceStoreProvider,
    capabilityProviders: List<CapabilityProvider>,
    backgroundScheduler: BackgroundScheduler? = null,
    continuousExecutionController: ContinuousExecutionController? = null,
    backgroundTaskHandlers: List<BackgroundTaskHandler> = emptyList(),
) {
    if (KoinPlatform.getKoinOrNull() != null) return

    startKoin {
        allowOverride(false)
        modules(
            carBrozApplicationModule(
                configuration = configuration,
                secureStorage = secureStorage,
                databaseProvider = databaseProvider,
                preferenceStoreProvider = preferenceStoreProvider,
                capabilityProviders = capabilityProviders,
                backgroundScheduler = backgroundScheduler,
                continuousExecutionController = continuousExecutionController,
                backgroundTaskHandlers = backgroundTaskHandlers,
            ),
        )
    }
}
