package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.observability.MainThreadDispatcher
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.foundation.observability.ResourceDiagnosticResult
import com.carbroz.foundation.observability.ResourceDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
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
    fun `development constructs and retains local operational diagnostics`() {
        var sinkFactoryCalls = 0
        var resourceFactoryCalls = 0
        var dispatcherFactoryCalls = 0

        val selected = localOperationalDiagnostics(
            environment = AppEnvironment.Development,
            platformSinks = {
                sinkFactoryCalls += 1
                sinks
            },
            resourceDiagnostics = {
                resourceFactoryCalls += 1
                resources
            },
            mainThreadDispatcher = {
                dispatcherFactoryCalls += 1
                dispatcher
            },
        )

        assertSame(sinks, selected.sinks)
        assertSame(resources, selected.resourceDiagnostics)
        assertSame(dispatcher, selected.mainThreadDispatcher)
        assertEquals(1, sinkFactoryCalls)
        assertEquals(1, resourceFactoryCalls)
        assertEquals(1, dispatcherFactoryCalls)
    }

    @Test
    fun `staging constructs and retains local operational diagnostics`() {
        val selected = localOperationalDiagnostics(
            environment = AppEnvironment.Staging,
            platformSinks = { sinks },
            resourceDiagnostics = { resources },
            mainThreadDispatcher = { dispatcher },
        )

        assertSame(sinks, selected.sinks)
        assertSame(resources, selected.resourceDiagnostics)
        assertSame(dispatcher, selected.mainThreadDispatcher)
    }

    @Test
    fun `production does not construct local diagnostics`() {
        var sinkFactoryCalls = 0
        var resourceFactoryCalls = 0
        var dispatcherFactoryCalls = 0

        val selected = localOperationalDiagnostics(
            environment = AppEnvironment.Production,
            platformSinks = {
                sinkFactoryCalls += 1
                sinks
            },
            resourceDiagnostics = {
                resourceFactoryCalls += 1
                resources
            },
            mainThreadDispatcher = {
                dispatcherFactoryCalls += 1
                dispatcher
            },
        )

        assertNotSame(sinks, selected.sinks)
        assertNull(selected.resourceDiagnostics)
        assertNull(selected.mainThreadDispatcher)
        assertEquals(0, sinkFactoryCalls)
        assertEquals(0, resourceFactoryCalls)
        assertEquals(0, dispatcherFactoryCalls)
    }
}
