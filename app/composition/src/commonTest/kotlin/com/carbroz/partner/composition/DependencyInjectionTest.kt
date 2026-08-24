package com.carbroz.partner.composition

import androidx.room3.RoomDatabase
import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseBuilderProvider
import com.carbroz.data.network.KtorNetworkTransport
import com.carbroz.data.network.NetworkAuthorizationProvider
import com.carbroz.data.network.NetworkEnvironment
import com.carbroz.data.network.NetworkEnvironmentProvider
import com.carbroz.data.network.NetworkExecutor
import com.carbroz.data.network.NetworkTransport
import com.carbroz.data.network.SessionNetworkAuthorizationProvider
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
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.startup.StartupCoordinator
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
    fun applicationModuleResolvesCanonicalRuntimeSessionAndNetworkGraph() {
        val secureStorage = FakeSecureStorage()
        val databaseBuilderProvider = FailingDatabaseBuilderProvider()
        val application = koinApplication {
            modules(carBrozApplicationModule(configuration, secureStorage, databaseBuilderProvider))
        }

        try {
            val koin = application.koin
            val controller = koin.get<AppLifecycleController>()
            val sessionStore = koin.get<SessionStore>()
            val networkEnvironmentProvider = koin.get<NetworkEnvironmentProvider>()
            val networkEnvironment = koin.get<NetworkEnvironment>()
            val ktorTransport = koin.get<KtorNetworkTransport>()
            val authorizationProvider = koin.get<NetworkAuthorizationProvider>()

            assertSame(controller, koin.get<AppLifecycle>())
            assertSame(configuration, koin.get<AppConfiguration>())
            assertEquals(configuration, koin.get<ConfigurationProvider>().get())
            assertSame(secureStorage, koin.get<SecureStorage>())
            assertSame(databaseBuilderProvider, koin.get<CarBrozDatabaseBuilderProvider>())
            assertSame(sessionStore, koin.get<SessionProvider>())
            koin.get<SessionSnapshotCodec>()
            koin.get<SessionPersistence>()
            koin.get<TokenExpiryPolicy>()
            koin.get<SessionRestoreStartupTask>()

            assertEquals(networkEnvironmentProvider.get(), networkEnvironment)
            assertSame(ktorTransport, koin.get<NetworkTransport>())
            assertIs<SessionNetworkAuthorizationProvider>(authorizationProvider)
            assertSame(authorizationProvider, koin.get<NetworkAuthorizationProvider>())
            koin.get<NetworkExecutor>()

            koin.get<StartupCoordinator>()
            koin.get<ApplicationRuntime>()
        } finally {
            application.close()
        }
    }

    @Test
    fun processInitializerIsIdempotent() {
        KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        val secureStorage = FakeSecureStorage()
        val databaseBuilderProvider = FailingDatabaseBuilderProvider()

        try {
            initializeCarBrozDependencyInjection(configuration, secureStorage, databaseBuilderProvider)
            val first = KoinPlatform.getKoinOrNull()
            initializeCarBrozDependencyInjection(configuration, secureStorage, databaseBuilderProvider)
            val second = KoinPlatform.getKoinOrNull()

            assertNotNull(first)
            assertSame(first, second)
        } finally {
            KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        }
    }

    private class FailingDatabaseBuilderProvider : CarBrozDatabaseBuilderProvider {
        override fun builder(): RoomDatabase.Builder<CarBrozDatabase> =
            error("Database creation is not required by this DI graph test")
    }

    private class FakeSecureStorage : SecureStorage {
        private val values = mutableMapOf<String, String>()

        override suspend fun read(key: SecureKey): String? = values[key.value]

        override suspend fun write(key: SecureKey, value: String) {
            values[key.value] = value
        }

        override suspend fun remove(key: SecureKey) {
            values.remove(key.value)
        }

        override suspend fun clear() {
            values.clear()
        }
    }
}
