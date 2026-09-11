package com.carbroz.partner.composition

import com.carbroz.data.bootstrap.RemoteBootstrapRepository
import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseProvider
import com.carbroz.data.database.DatabaseHealthCheck
import com.carbroz.data.database.RoomDatabaseHealthCheck
import com.carbroz.data.network.ConfigurationNetworkHeaderProvider
import com.carbroz.data.network.DiagnosticNetworkTransport
import com.carbroz.data.network.ExecutorNetworkDataSource
import com.carbroz.data.network.InMemoryNetworkResponseCache
import com.carbroz.data.network.KtorNetworkTransport
import com.carbroz.data.network.NetworkAuthenticationRecovery
import com.carbroz.data.network.NetworkAuthorizationProvider
import com.carbroz.data.network.NetworkConnectivityObserver
import com.carbroz.data.network.NetworkConnectivityProvider
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkEnvironmentProvider
import com.carbroz.data.network.NetworkExecutor
import com.carbroz.data.network.NetworkHeaderProvider
import com.carbroz.data.network.NetworkRequestId
import com.carbroz.data.network.NetworkRequestIdProvider
import com.carbroz.data.network.NetworkResponseCache
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.SessionNetworkAuthenticationRecovery
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
import com.carbroz.feature.dynamic.BackgroundActionExecutor
import com.carbroz.feature.dynamic.CapabilityActionExecutor
import com.carbroz.feature.dynamic.DefaultDynamicBindingContextFactory
import com.carbroz.feature.dynamic.DynamicBindingContextFactory
import com.carbroz.feature.dynamic.DynamicFeatureFactory
import com.carbroz.feature.dynamic.DynamicScreenCache
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.dynamic.NetworkActionExecutor
import com.carbroz.feature.splash.SplashDestination
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
import com.carbroz.foundation.observability.CorrelationIdProvider
import com.carbroz.foundation.observability.DiagnosticBlockSink
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.MainThreadDispatcher
import com.carbroz.foundation.observability.MainThreadResponsivenessMonitor
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ObservabilityPolicy
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.foundation.observability.RandomCorrelationIdProvider
import com.carbroz.foundation.observability.ResourceDiagnostics
import com.carbroz.foundation.observability.ResourceDiagnosticsReporter
import com.carbroz.foundation.security.SecureStorage
import com.carbroz.foundation.session.JsonSessionSnapshotCodec
import com.carbroz.foundation.session.SecureSessionPersistence
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionRefreshCoordinator
import com.carbroz.foundation.session.SessionSnapshotCodec
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SingleFlightTokenRefresher
import com.carbroz.foundation.session.TokenExpiryPolicy
import com.carbroz.foundation.session.TokenRefresher
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import com.carbroz.platform.background.BackgroundScheduler
import com.carbroz.platform.background.BackgroundTaskHandler
import com.carbroz.platform.background.BackgroundTaskHandlerRegistry
import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.ContinuousExecutionController
import com.carbroz.runtime.action.ActionPreparerFactory
import com.carbroz.runtime.application.startup.BootstrapRepository
import com.carbroz.runtime.application.startup.PartnerConfigStore
import com.carbroz.runtime.application.startup.ResolveStartupUseCase
import com.carbroz.runtime.sdui.SduiRuntime
import com.carbroz.runtime.sdui.SduiRuntimeFactory
import com.carbroz.runtime.sdui.compatibility.SduiClientCompatibility
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

fun carBrozApplicationModule(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
    databaseProvider: CarBrozDatabaseProvider,
    preferenceStoreProvider: PreferenceStoreProvider,
    capabilityProviders: List<CapabilityProvider> = emptyList(),
    backgroundScheduler: BackgroundScheduler? = null,
    continuousExecutionController: ContinuousExecutionController? = null,
    backgroundTaskHandlers: List<BackgroundTaskHandler> = emptyList(),
    observabilitySinks: ObservabilitySinks = ObservabilitySinks(),
    resourceDiagnostics: ResourceDiagnostics? = null,
    mainThreadDispatcher: MainThreadDispatcher? = null,
) = module {
    single { configuration }
    single<ConfigurationProvider> { ConfigurationProvider { get<AppConfiguration>() } }
    single<DiagnosticBlockSink> { observabilitySinks.diagnostic }

    single<CorrelationIdProvider> { RandomCorrelationIdProvider() }
    single {
        Observability(
            policy = ObservabilityPolicy(
                minimumLogLevel = when (configuration.environment) {
                    AppEnvironment.Development,
                    AppEnvironment.Staging,
                    -> LogLevel.DEBUG
                    AppEnvironment.Production -> LogLevel.WARN
                },
                crashReportingEnabled = true,
                performanceMetricsEnabled = true,
                tracingEnabled = true,
                responsivenessReportingEnabled = true,
                resourceDiagnosticsEnabled = true,
                includeThrowableDetails = false,
            ),
            logSink = observabilitySinks.log,
            crashSink = observabilitySinks.crash,
            performanceSink = observabilitySinks.performance,
            traceSink = observabilitySinks.trace,
            responsivenessSink = observabilitySinks.responsiveness,
            resourceSink = observabilitySinks.resource,
        )
    }
    if (resourceDiagnostics != null) {
        single<ResourceDiagnostics> { resourceDiagnostics }
        single { ResourceDiagnosticsReporter(diagnostics = get(), observability = get()) }
    }
    if (mainThreadDispatcher != null) {
        single { MainThreadResponsivenessMonitor(dispatcher = mainThreadDispatcher, observability = get()) }
    }
    single { AnalyticsTracker(AnalyticsPolicy(enabled = false, allowedEventNames = emptySet())) }

    single<Clock> { SystemClock }
    single<SecureStorage> { secureStorage }
    single<SessionSnapshotCodec> { JsonSessionSnapshotCodec() }
    single<SessionPersistence> { SecureSessionPersistence(secureStorage = get(), codec = get()) }
    single { SessionStore(persistence = get()) }
    single<SessionProvider> { get<SessionStore>() }
    single { TokenExpiryPolicy(clock = get()) }

    single<CarBrozDatabaseProvider> { databaseProvider }
    single<CarBrozDatabase> { get<CarBrozDatabaseProvider>().get() }
    single<DatabaseHealthCheck> { RoomDatabaseHealthCheck(database = get(), observability = get(), clock = get()) }
    single<PreferenceStoreProvider> { preferenceStoreProvider }
    single<PreferenceStore> { get<PreferenceStoreProvider>().get() }

    single<CapabilityRegistry> { createCapabilityRegistry(capabilityProviders) }
    single { CapabilityActionExecutor(registry = get()) }

    single { BackgroundTaskHandlerRegistry(backgroundTaskHandlers) }
    single { BackgroundTaskRunner(registry = get(), observability = get(), clock = get(), correlationIdProvider = get()) }
    if (backgroundScheduler != null) single<BackgroundScheduler> { backgroundScheduler }
    if (continuousExecutionController != null) single<ContinuousExecutionController> { continuousExecutionController }
    single { BackgroundActionExecutor(backgroundScheduler, continuousExecutionController) }

    single { ApplicationConnectivity() }
    single<NetworkConnectivityObserver> { get<ApplicationConnectivity>() }
    single<NetworkConnectivityProvider> { get<ApplicationConnectivity>() }
    single { NetworkEnvironmentProvider(configurationProvider = get()) }
    single<NetworkEnvironment> { get<NetworkEnvironmentProvider>().get() }
    single { createKtorNetworkTransport() }
    single<NetworkTransport> {
        when (configuration.environment) {
            AppEnvironment.Development,
            AppEnvironment.Staging,
            -> DiagnosticNetworkTransport(
                delegate = get<KtorNetworkTransport>(),
                sink = get(),
            )
            AppEnvironment.Production -> get<KtorNetworkTransport>()
        }
    }
    single<NetworkAuthorizationProvider> { SessionNetworkAuthorizationProvider(sessionProvider = get()) }
    single<NetworkHeaderProvider> { ConfigurationNetworkHeaderProvider(configurationProvider = get()) }

    single { SessionRefreshScope() }
    single<TokenRefresher> {
        SingleFlightTokenRefresher(
            delegate = CarBrozTokenRefresher(
                environment = get(),
                transport = get(),
                clock = get(),
            ),
            scope = get<SessionRefreshScope>().coroutineScope,
        )
    }
    single {
        SessionRefreshCoordinator(
            sessionStore = get(),
            expiryPolicy = get(),
            tokenRefresher = get(),
        )
    }
    single<NetworkAuthenticationRecovery> {
        SessionNetworkAuthenticationRecovery(
            coordinator = get(),
            sessionStore = get(),
        )
    }

    single<NetworkResponseCache> { InMemoryNetworkResponseCache() }
    single<NetworkRequestIdProvider> {
        val correlationIds = get<CorrelationIdProvider>()
        NetworkRequestIdProvider { NetworkRequestId(correlationIds.next("network").value) }
    }
    single {
        NetworkExecutor(
            environment = get(),
            transport = get(),
            headerProvider = get(),
            authorizationProvider = get(),
            authenticationRecovery = get(),
            connectivityProvider = get(),
            requestIdProvider = get(),
            responseCache = get(),
            clock = get(),
            observability = get(),
        )
    }
    single<NetworkDataSource> { ExecutorNetworkDataSource(executor = get()) }
    single { NetworkActionExecutor(dataSource = get()) }

    single<BootstrapRepository> { RemoteBootstrapRepository(network = get()) }
    single { PartnerConfigStore() }
    single {
        ResolveStartupUseCase(
            sessionStore = get(),
            bootstrapRepository = get(),
            partnerConfigStore = get(),
        )
    }

    single<SduiRuntime> {
        SduiRuntimeFactory.createCore(
            SduiClientCompatibility(
                clientVersion = configuration.buildInformation.versionCode
                    .coerceIn(1L, Int.MAX_VALUE.toLong())
                    .toInt(),
                supportedProtocolVersions = 1..1,
                supportedSchemaVersions = 1..1,
            ),
        )
    }
    single { ActionPreparerFactory.createCore() }
    single<DynamicBindingContextFactory> {
        DefaultDynamicBindingContextFactory(sessionProvider = get(), configurationProvider = get())
    }
    single { FormTemplateRuntimeFactory(get<SduiRuntime>().registry) }
    single { DynamicScreenCache() }
    single { DynamicScreenInstructionCodec() }
    single {
        DynamicFeatureFactory(
            sduiRuntime = get(),
            actionPreparer = get(),
            networkActions = get(),
            capabilityActions = get(),
            backgroundActions = get(),
            navigation = get(),
            bindingContexts = get(),
            formRuntime = get(),
            cache = get(),
        )
    }

    single { createKtorRealtimeTransport() }
    single<RealtimeTransport> { get<KtorRealtimeTransport>() }
    single { RealtimeStream(transport = get()) }
    single { RealtimeDeliveryGate() }

    single<OutboxStore> { RoomOutboxStore(database = get()) }
    single<SyncConflictResolver> { KeepQueuedSyncConflictResolver }
    single<SyncCoordinator> {
        DefaultSyncCoordinator(outbox = get(), network = get(), clock = get(), conflictResolver = get())
    }
    single { SyncActivationPolicy() }

    single { NavigationStore(NavigationState(listOf(SplashDestination))) }
    single { DefaultAppLifecycle() } bind AppLifecycleController::class
    single<AppLifecycle> { get<AppLifecycleController>() }
    single {
        SyncActivationCoordinator(
            lifecycle = get(),
            connectivity = get(),
            syncCoordinator = get(),
            policy = get(),
        )
    }
}

internal fun initializeCarBrozDependencyInjection(
    configuration: AppConfiguration,
    secureStorage: SecureStorage,
    databaseProvider: CarBrozDatabaseProvider,
    preferenceStoreProvider: PreferenceStoreProvider,
    capabilityProviders: List<CapabilityProvider>,
    backgroundScheduler: BackgroundScheduler? = null,
    continuousExecutionController: ContinuousExecutionController? = null,
    backgroundTaskHandlers: List<BackgroundTaskHandler> = emptyList(),
    observabilitySinks: ObservabilitySinks = ObservabilitySinks(),
    resourceDiagnostics: ResourceDiagnostics? = null,
    mainThreadDispatcher: MainThreadDispatcher? = null,
) {
    if (KoinPlatform.getKoinOrNull() != null) return
    val application = startKoin {
        allowOverride(false)
        modules(
            carBrozApplicationModule(
                configuration,
                secureStorage,
                databaseProvider,
                preferenceStoreProvider,
                capabilityProviders,
                backgroundScheduler,
                continuousExecutionController,
                backgroundTaskHandlers,
                observabilitySinks,
                resourceDiagnostics,
                mainThreadDispatcher,
            ),
        )
    }
    if (resourceDiagnostics != null) application.koin.get<ResourceDiagnosticsReporter>().sample()
    if (mainThreadDispatcher != null) application.koin.get<MainThreadResponsivenessMonitor>().start()
}
