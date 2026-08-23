package com.carbroz.partner.composition

import com.carbroz.foundation.lifecycle.AppLifecycle
import com.carbroz.foundation.lifecycle.AppLifecycleController
import com.carbroz.runtime.application.ApplicationRuntime
import com.carbroz.runtime.application.startup.StartupCoordinator
import kotlin.test.Test
import kotlin.test.assertSame
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.koin.dsl.koinApplication

class DependencyInjectionTest {
    @Test
    fun applicationModuleResolvesCanonicalRuntimeGraph() {
        val application = koinApplication {
            modules(carBrozApplicationModule)
        }

        try {
            val koin = application.koin
            val controller = koin.get<AppLifecycleController>()

            assertSame(controller, koin.get<AppLifecycle>())
            koin.get<StartupCoordinator>()
            koin.get<ApplicationRuntime>()
        } finally {
            application.close()
        }
    }

    @Test
    fun processInitializerIsIdempotent() {
        GlobalContext.getOrNull()?.let { stopKoin() }

        try {
            val first = initializeCarBrozDependencyInjection()
            val second = initializeCarBrozDependencyInjection()

            assertSame(first, second)
        } finally {
            GlobalContext.getOrNull()?.let { stopKoin() }
        }
    }
}
