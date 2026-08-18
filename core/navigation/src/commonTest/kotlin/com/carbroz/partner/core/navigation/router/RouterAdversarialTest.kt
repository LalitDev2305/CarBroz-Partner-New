package com.carbroz.partner.core.navigation.router

import com.carbroz.partner.core.navigation.command.NavCommand
import com.carbroz.partner.core.navigation.command.PopToTarget
import com.carbroz.partner.core.navigation.destination.NavDestination
import com.carbroz.partner.core.navigation.result.NavResult
import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RouterAdversarialTest {

    private class TestLogger : StructuredLogger {
        val events = mutableListOf<LogEvent>()

        private inner class TestBoundLogger(private val sourceClass: String) : BoundLogger {
            override fun log(
                level: LogLevel,
                category: LogCategory,
                sourceFunction: String,
                event: String,
                message: String,
                attributes: Map<String, LogAttribute>,
                traceContext: TraceContext?,
                durationMs: Long?,
                throwable: Throwable?
            ) {
                events.add(
                    LogEvent(
                        timestampMs = 0L,
                        level = level,
                        category = category,
                        sourceClass = sourceClass,
                        sourceFunction = sourceFunction,
                        event = event,
                        message = message,
                        attributes = emptyMap(),
                        traceContext = traceContext,
                        durationMs = durationMs,
                        errorInfo = null
                    )
                )
            }

            override fun info(sourceFunction: String, category: LogCategory, event: String, message: String, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                log(LogLevel.INFO, category, sourceFunction, event, message, attributes, traceContext, null, null)
            }

            override fun debug(sourceFunction: String, category: LogCategory, event: String, message: String, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                log(LogLevel.DEBUG, category, sourceFunction, event, message, attributes, traceContext, null, null)
            }

            override fun error(sourceFunction: String, category: LogCategory, event: String, message: String, throwable: Throwable?, attributes: Map<String, LogAttribute>, traceContext: TraceContext?) {
                log(LogLevel.ERROR, category, sourceFunction, event, message, attributes, traceContext, null, throwable)
            }
        }

        override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger {
            return TestBoundLogger(sourceClass)
        }

        override fun isLevelEnabled(level: LogLevel): Boolean = true
    }

    private val rootDestination = NavDestination.create("splash")

    @Test
    fun verifyPopToInclusiveRootProtection() = runTest {
        val router = DefaultRouter(rootDestination)
        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("splash"), inclusive = true))

        assertIs<NavResult.Rejected.CannotPopRoot>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("splash", router.currentState.activeEntry.destination.route)
    }

    @Test
    fun verifyDuplicateRouteEntriesHaveUniqueIDs() = runTest {
        val router = DefaultRouter(rootDestination)
        val route = NavDestination.create("booking", mapOf("id" to "BK1"))
        
        router.execute(NavCommand.Push(route))
        val entry1 = router.currentState.activeEntry
        
        router.execute(NavCommand.Push(route))
        val entry2 = router.currentState.activeEntry

        assertEquals(3, router.currentState.stack.size)
        assertNotEquals(entry1.entryId, entry2.entryId)
    }

    @Test
    fun verifySameRouteDifferentParamsCoexist() = runTest {
        val router = DefaultRouter(rootDestination)
        val dest1 = NavDestination.create("booking", mapOf("id" to "BK1"))
        val dest2 = NavDestination.create("booking", mapOf("id" to "BK2"))

        router.execute(NavCommand.Push(dest1))
        router.execute(NavCommand.Push(dest2))

        assertEquals(3, router.currentState.stack.size)
        assertEquals("BK2", router.currentState.activeEntry.destination.params["id"])
    }

    @Test
    fun verifyInvalidRouteLeavesStateUnchanged() = runTest {
        val router = DefaultRouter(rootDestination)
        val initialEntry = router.currentState.activeEntry

        val result = router.execute(NavCommand.Push(NavDestination.create("")))

        assertIs<NavResult.Rejected.InvalidRoute>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals(initialEntry, router.currentState.activeEntry)
    }

    @Test
    fun verifyPopRootRejectionPreservesExactNavState() = runTest {
        val router = DefaultRouter(rootDestination)
        val stateBefore = router.currentState

        val result = router.execute(NavCommand.Pop)

        assertIs<NavResult.Rejected.CannotPopRoot>(result)
        assertEquals(stateBefore, router.currentState)
        assertEquals(stateBefore.activeEntry, router.currentState.activeEntry)
    }

    @Test
    fun verifyPopToMissingTargetRejectionPreservesExactNavState() = runTest {
        val router = DefaultRouter(rootDestination)
        val stateBefore = router.currentState

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("missing_route")))

        assertIs<NavResult.Rejected.TargetNotFound>(result)
        assertEquals(stateBefore, router.currentState)
    }

    @Test
    fun verifyBothBookingAndCredentialSecretsAbsentFromLogs() = runTest {
        val logger = TestLogger()
        val router = DefaultRouter(rootDestination, logger = logger)
        val bookingSecret = "SUPER_SECRET_BOOKING_123"
        val credentialSecret = "PRIVATE_CREDENTIAL_987"
        val dest = NavDestination.create("booking", mapOf("token" to credentialSecret, "booking" to bookingSecret))

        router.execute(NavCommand.Push(dest))

        assertTrue(logger.events.none { it.message.contains(bookingSecret) })
        assertTrue(logger.events.none { it.message.contains(credentialSecret) })
    }

    @Test
    fun verifyStateFlowValueMatchesCurrentStateAndEmitsOnMutation() = runTest {
        val router = DefaultRouter(rootDestination)
        assertEquals(router.state.value, router.currentState)

        val nextDest = NavDestination.create("dashboard")
        router.execute(NavCommand.Push(nextDest))

        assertEquals(router.state.value, router.currentState)
        assertEquals("dashboard", router.currentState.activeEntry.destination.route)
    }
}

