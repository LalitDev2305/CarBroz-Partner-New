package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.observability.MainThreadDispatcher
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.foundation.observability.ResourceDiagnosticResult
import com.carbroz.foundation.observability.ResourceDiagnostics
import kotlin.test.Test
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class OperationalEnvironmentTest {
    private val resources = ResourceDiagnostics {
        ResourceDiagnosticResult.Unsupported("test")
    }
    private val dispatcher = MainThreadDispatcher { it() }
    private val sinks = ObservabilitySinks()

    @Test
    fun `development retains local operational diagnostics`() {
        val selected = localOperationalDiagnostics(
            environment = AppEnvironment.Development,
            platformSinks = sinks,
            resourceDiagnostics = resources,
            mainThreadDispatcher = dispatcher,
        )

        assertSame(sinks, selected.sinks)
        assertSame(resources, selected.resourceDiagnostics)
        assertSame(dispatcher, selected.mainThreadDispatcher)
    }

    @Test
    fun `production defaults to neutral sinks without local watchdog or sampling`() {
        val selected = localOperationalDiagnostics(
            environment = AppEnvironment.Production,
            platformSinks = sinks,
            resourceDiagnostics = resources,
            mainThreadDispatcher = dispatcher,
        )

        assertNotSame(sinks, selected.sinks)
        assertNull(selected.resourceDiagnostics)
        assertNull(selected.mainThreadDispatcher)
    }
}
