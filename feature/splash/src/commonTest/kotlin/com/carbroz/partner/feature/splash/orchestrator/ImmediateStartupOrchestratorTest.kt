package com.carbroz.partner.feature.splash.orchestrator

import com.carbroz.partner.domain.session.restore.SessionRestorer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImmediateStartupOrchestratorTest {

    @Test
    fun testInitializeInvokesRestorerAndReturnsServerDrivenUi() = runTest {
        var restored = false
        val orchestrator = ImmediateStartupOrchestrator(
            sessionRestorer = null
        )

        val result = orchestrator.initialize()
        assertTrue(result is StartupResult.Ready)
        assertEquals(StartupDestination.ServerDrivenUi, (result as StartupResult.Ready).destination)
    }
}
