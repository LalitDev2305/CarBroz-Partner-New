package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseProvider
import com.carbroz.data.network.KtorNetworkTransport
import com.carbroz.data.network.NetworkAuthorizationProvider
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkEnvironmentProvider
import com.carbroz.data.network.NetworkExecutor
import com.carbroz.data.network.NetworkResponseCache
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.SessionNetworkAuthorizationProvider
import com.carbroz.data.preferences.PreferenceKey
import com.carbroz.data.preferences.PreferenceStore
import com.carbroz.data.preferences.PreferenceStoreProvider
import com.carbroz.data.realtime.KtorRealtimeTransport
import com.carbroz.data.realtime.RealtimeDeliveryGate
import com.carbroz.data.realtime.RealtimeStream
import com.carbroz.data.realtime.RealtimeTransport
import com.carbroz.feature.dynamic.BackgroundActionExecutor
import com.carbroz.feature.dynamic.CapabilityActionExecutor
import com.carbroz.feature.dynamic.DynamicBindingContextFactory
import com.carbroz.feature.dynamic.DynamicFeatureFactory
import com.carbroz.feature.dynamic.DynamicScreenCache
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.dynamic.NetworkActionExecutor
import com.carbroz.feature.splash.BootstrapClientCapabilities
import com.carbroz.feature.splash.BootstrapConfigurationCache
import com.carbroz.feature.splash.BootstrapConfigurationStartupTask
import com.carbroz.feature.splash.BootstrapStore
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import com.carbroz.foundation.session.SessionPersistence
import com.carbroz.foundation.session.SessionProvider
import com.carbroz.foundation.session.SessionSnapshotCodec
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.TokenExpiryPolicy
import com.carbroz.platform.background.BackgroundExecutionResult
import com.carbroz.platform.background.BackgroundScheduleResult
import com.carbroz.platform.background.BackgroundScheduler
import com.carbroz.platform.background.BackgroundTaskHandler
import com.carbroz.platform.background.BackgroundTaskHandlerRegistry
import com.carbroz.platform.background.BackgroundTaskId
import com.carbroz.platform.background.BackgroundTaskRequest
import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.BackgroundTaskState
import com.carbroz.platform.background.ContinuousExecutionController
import com.carbroz.platform.background.ContinuousExecutionRequest
import com.carbroz.platform.background.ContinuousExecutionStartResult
import com.carbroz.platform.background.ContinuousExecutionState
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.sdui.SduiRuntime
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication
import org.koin.mp.KoinPlatform

class DependencyInjectionTest {
    private val configuration = AppConfiguration(
        environment = AppEnvironment.Development,
        apiBaseUrl = "https://development.invalid",
        buildInformation = BuildInformation(
            versionName = "1.0.0-dev",
            versionCode = 1L,
            applicationId = "com.carbroz.partner.dev",
        ),
    )

    @Test
    fun applicationModuleResolvesCanonicalRuntimeSessionNetworkRealtimeAndFeatureGraph() {
        val secureStorage = FakeSecureStorage()
        val databaseProvider = FailingDatabaseProvider()
        val preferenceStore = FakePreferenceStore()
        val preferenceProvider = PreferenceStoreProvider { preferenceStore }
        val application = koinApplication {
            modules(carBrozApplicationModule(configuration, secureStorage, databaseProvider, preferenceProvider))
        }

        try {
            val koin = application.koin
            val controller = koin.get<AppLifecycleController>()
            val sessionStore = koin.get<SessionStore>()
            val networkEnvironmentProvider = koin.get<NetworkEnvironmentProvider>()
            val networkEnvironment = koin.get<NetworkEnvironment>()
            val ktorTransport = koin.get<KtorNetworkTransport>()
            val authorizationProvider = koin.get<NetworkAuthorizationProvider>()
            val realtimeTransport = koin.get<KtorRealtimeTransport>()

            assertSame(controller, koin.get<AppLifecycle>())
            assertSame(configuration, koin.get<AppConfiguration>())
            assertEquals(configuration, koin.get<ConfigurationProvider>().get())
            assertSame(secureStorage, koin.get<SecureStorage>())
            assertSame(databaseProvider, koin.get<CarBrozDatabaseProvider>())
            assertSame(preferenceProvider, koin.get<PreferenceStoreProvider>())
            assertSame(preferenceStore, koin.get<PreferenceStore>())
            assertSame(sessionStore, koin.get<SessionProvider>())
            koin.get<SessionSnapshotCodec>()
            koin.get<SessionPersistence>()
            koin.get<TokenExpiryPolicy>()
            koin.get<SessionRestoreStartupTask>()

            assertEquals(networkEnvironmentProvider.get(), networkEnvironment)
            assertSame(ktorTransport, koin.get<NetworkTransport>())
            assertIs<SessionNetworkAuthorizationProvider>(authorizationProvider)
            koin.get<NetworkResponseCache>()
            koin.get<NetworkExecutor>()
            koin.get<NetworkDataSource>()

            koin.get<NetworkActionExecutor>()
            koin.get<CapabilityActionExecutor>()
            koin.get<BackgroundActionExecutor>()
            koin.get<DynamicScreenInstructionCodec>()
            koin.get<SduiRuntime>()
            koin.get<ActionPreparer>()
            koin.get<DynamicBindingContextFactory>()
            koin.get<FormTemplateRuntimeFactory>()
            koin.get<DynamicScreenCache>()
            koin.get<DynamicFeatureFactory>()
            koin.get<BootstrapStore>()
            koin.get<BootstrapConfigurationCache>()
            val client = koin.get<BootstrapClientCapabilities>()
            assertEquals(configuration.buildInformation.versionCode, client.versionCode)
            koin.get<BootstrapConfigurationStartupTask>()

            assertSame(realtimeTransport, koin.get<RealtimeTransport>())
            koin.get<RealtimeStream>()
            koin.get<RealtimeDeliveryGate>()

            koin.get<BackgroundTaskHandlerRegistry>()
            koin.get<BackgroundTaskRunner>()
            koin.get<StartupCoordinator>()
            koin.get<ApplicationRuntime>()
        } finally {
            application.close()
        }
    }

    @Test
    fun applicationModuleUsesSinglePlatformBackgroundSchedulerAndContinuousController() {
        val scheduler = FakeBackgroundScheduler()
        val controller = FakeContinuousExecutionController()
        val handler = object : BackgroundTaskHandler {
            override val id = BackgroundTaskId("test-handler")
            override suspend fun execute(input: Map<String, String>): BackgroundExecutionResult =
                BackgroundExecutionResult.Success
        }
        val application = koinApplication {
            modules(
                carBrozApplicationModule(
                    configuration = configuration,
                    secureStorage = FakeSecureStorage(),
                    databaseProvider = FailingDatabaseProvider(),
                    preferenceStoreProvider = PreferenceStoreProvider { FakePreferenceStore() },
                    backgroundScheduler = scheduler,
                    continuousExecutionController = controller,
                    backgroundTaskHandlers = listOf(handler),
                ),
            )
        }

        try {
            assertSame(scheduler, application.koin.get<BackgroundScheduler>())
            assertSame(controller, application.koin.get<ContinuousExecutionController>())
            assertSame(handler, application.koin.get<BackgroundTaskHandlerRegistry>().handler(handler.id))
        } finally {
            application.close()
        }
    }

    @Test
    fun processInitializerIsIdempotent() {
        KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        val secureStorage = FakeSecureStorage()
        val databaseProvider = FailingDatabaseProvider()
        val preferenceProvider = PreferenceStoreProvider { FakePreferenceStore() }

        try {
            initializeCarBrozDependencyInjection(configuration, secureStorage, databaseProvider, preferenceProvider, emptyList())
            val first = KoinPlatform.getKoinOrNull()
            initializeCarBrozDependencyInjection(configuration, secureStorage, databaseProvider, preferenceProvider, emptyList())
            val second = KoinPlatform.getKoinOrNull()

            assertNotNull(first)
            assertSame(first, second)
        } finally {
            KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        }
    }

    private class FailingDatabaseProvider : CarBrozDatabaseProvider {
        override fun get(): CarBrozDatabase = error("Database creation is not required by this DI graph test")
    }

    private class FakePreferenceStore : PreferenceStore {
        override fun <T> observe(key: PreferenceKey<T>): Flow<T?> = flowOf(null)
        override suspend fun <T> get(key: PreferenceKey<T>): T? = null
        override suspend fun <T> put(key: PreferenceKey<T>, value: T) = Unit
        override suspend fun <T> remove(key: PreferenceKey<T>) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeSecureStorage : SecureStorage {
        private val values = mutableMapOf<String, String>()
        override suspend fun read(key: SecureKey): String? = values[key.value]
        override suspend fun write(key: SecureKey, value: String) { values[key.value] = value }
        override suspend fun remove(key: SecureKey) { values.remove(key.value) }
        override suspend fun clear() { values.clear() }
    }

    private class FakeBackgroundScheduler : BackgroundScheduler {
        override suspend fun schedule(request: BackgroundTaskRequest): BackgroundScheduleResult = BackgroundScheduleResult.Scheduled
        override suspend fun cancel(id: BackgroundTaskId) = Unit
        override suspend fun state(id: BackgroundTaskId): BackgroundTaskState = BackgroundTaskState.UNKNOWN
    }

    private class FakeContinuousExecutionController : ContinuousExecutionController {
        override suspend fun start(request: ContinuousExecutionRequest): ContinuousExecutionStartResult = ContinuousExecutionStartResult.Started
        override suspend fun stop(id: BackgroundTaskId) = Unit
        override suspend fun state(id: BackgroundTaskId): ContinuousExecutionState = ContinuousExecutionState.STOPPED
    }
}
