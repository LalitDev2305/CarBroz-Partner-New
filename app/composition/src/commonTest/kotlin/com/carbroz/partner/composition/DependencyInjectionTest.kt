package com.carbroz.partner.composition

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
    fun applicationModuleResolvesCanonicalRuntimeAndSessionGraph() {
        val secureStorage = FakeSecureStorage()
        val application = koinApplication {
            modules(carBrozApplicationModule(configuration, secureStorage))
        }

        try {
            val koin = application.koin
            val controller = koin.get<AppLifecycleController>()
            val sessionStore = koin.get<SessionStore>()

            assertSame(controller, koin.get<AppLifecycle>())
            assertSame(configuration, koin.get<AppConfiguration>())
            assertEquals(configuration, koin.get<ConfigurationProvider>().get())
            assertSame(secureStorage, koin.get<SecureStorage>())
            assertSame(sessionStore, koin.get<SessionProvider>())
            koin.get<SessionSnapshotCodec>()
            koin.get<SessionPersistence>()
            koin.get<TokenExpiryPolicy>()
            koin.get<SessionRestoreStartupTask>()
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

        try {
            initializeCarBrozDependencyInjection(configuration, secureStorage)
            val first = KoinPlatform.getKoinOrNull()
            initializeCarBrozDependencyInjection(configuration, secureStorage)
            val second = KoinPlatform.getKoinOrNull()

            assertNotNull(first)
            assertSame(first, second)
        } finally {
            KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        }
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
