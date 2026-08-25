package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.CarBrozDatabaseProvider
import com.carbroz.data.network.NetworkRequestIdProvider
import com.carbroz.data.preferences.PreferenceKey
import com.carbroz.data.preferences.PreferenceStore
import com.carbroz.data.preferences.PreferenceStoreProvider
import com.carbroz.foundation.analytics.AnalyticsTracker
import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.observability.CorrelationIdProvider
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.ResourceDiagnosticsReporter
import com.carbroz.foundation.security.SecureKey
import com.carbroz.foundation.security.SecureStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

class OperationalCompositionTest {
    @Test
    fun `composition owns one observability analytics and correlation graph`() {
        val application = koinApplication {
            modules(
                carBrozApplicationModule(
                    configuration = AppConfiguration(
                        environment = AppEnvironment.Development,
                        apiBaseUrl = "https://development.invalid",
                        buildInformation = BuildInformation("1", 1L, "com.carbroz.test"),
                    ),
                    secureStorage = FakeSecureStorage(),
                    databaseProvider = FailingDatabaseProvider(),
                    preferenceStoreProvider = PreferenceStoreProvider { FakePreferenceStore() },
                ),
            )
        }

        try {
            val koin = application.koin
            assertSame(koin.get<Observability>(), koin.get<Observability>())
            assertSame(koin.get<AnalyticsTracker>(), koin.get<AnalyticsTracker>())
            assertSame(koin.get<CorrelationIdProvider>(), koin.get<CorrelationIdProvider>())
            assertSame(koin.get<ResourceDiagnosticsReporter>(), koin.get<ResourceDiagnosticsReporter>())

            val requestIds = koin.get<NetworkRequestIdProvider>()
            assertNotEquals(requestIds.nextId(), requestIds.nextId())
        } finally {
            application.close()
        }
    }

    private class FailingDatabaseProvider : CarBrozDatabaseProvider {
        override fun get(): CarBrozDatabase = error("Database is not required by this graph test")
    }

    private class FakePreferenceStore : PreferenceStore {
        override fun <T> observe(key: PreferenceKey<T>): Flow<T?> = flowOf(null)
        override suspend fun <T> get(key: PreferenceKey<T>): T? = null
        override suspend fun <T> put(key: PreferenceKey<T>, value: T) = Unit
        override suspend fun <T> remove(key: PreferenceKey<T>) = Unit
        override suspend fun clear() = Unit
    }

    private class FakeSecureStorage : SecureStorage {
        override suspend fun read(key: SecureKey): String? = null
        override suspend fun write(key: SecureKey, value: String) = Unit
        override suspend fun remove(key: SecureKey) = Unit
        override suspend fun clear() = Unit
    }
}
