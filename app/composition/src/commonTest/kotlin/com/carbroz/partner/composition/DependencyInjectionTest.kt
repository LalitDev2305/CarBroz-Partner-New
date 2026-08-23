package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ConfigurationProvider
import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
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
    fun applicationModuleResolvesCanonicalRuntimeGraph() {
        val application = koinApplication {
            modules(carBrozApplicationModule(configuration))
        }

        try {
            val koin = application.koin
            val controller = koin.get<AppLifecycleController>()

            assertSame(controller, koin.get<AppLifecycle>())
            assertSame(configuration, koin.get<AppConfiguration>())
            assertEquals(configuration, koin.get<ConfigurationProvider>().get())
            koin.get<StartupCoordinator>()
            koin.get<ApplicationRuntime>()
        } finally {
            application.close()
        }
    }

    @Test
    fun processInitializerIsIdempotent() {
        KoinPlatform.getKoinOrNull()?.let { stopKoin() }

        try {
            initializeCarBrozDependencyInjection(configuration)
            val first = KoinPlatform.getKoinOrNull()
            initializeCarBrozDependencyInjection(configuration)
            val second = KoinPlatform.getKoinOrNull()

            assertNotNull(first)
            assertSame(first, second)
        } finally {
            KoinPlatform.getKoinOrNull()?.let { stopKoin() }
        }
    }
}
