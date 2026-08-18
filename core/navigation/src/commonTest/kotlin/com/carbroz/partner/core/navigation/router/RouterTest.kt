package com.carbroz.partner.core.navigation.router



import com.carbroz.partner.core.navigation.command.NavCommand
import com.carbroz.partner.core.navigation.command.PopToTarget
import com.carbroz.partner.core.navigation.destination.NavDestination
import com.carbroz.partner.core.navigation.result.NavResult
import com.carbroz.partner.core.navigation.stack.NavEntryIdGenerator
import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.AttributeSensitivity
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
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

class RouterTest {

    private class SequentialIdGenerator : NavEntryIdGenerator {
        private var id = 0
        override fun generateId(): String = "test_entry_${++id}"
    }

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
    fun verifyInitialRootSet() {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)

        assertEquals(1, router.currentState.stack.size)
        assertEquals("splash", router.currentState.activeEntry.destination.route)
        assertEquals("test_entry_1", router.currentState.activeEntry.entryId)
    }

    @Test
    fun verifyPushAppendsEntry() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        val homeDest = NavDestination.create("home")

        val result = router.execute(NavCommand.Push(homeDest))

        assertIs<NavResult.Executed>(result)
        assertEquals(2, router.currentState.stack.size)
        assertEquals("home", router.currentState.activeEntry.destination.route)
        assertEquals("test_entry_2", router.currentState.activeEntry.entryId)
    }

    @Test
    fun verifyPopRemovesTop() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        router.execute(NavCommand.Push(NavDestination.create("home")))

        val result = router.execute(NavCommand.Pop)

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("splash", router.currentState.activeEntry.destination.route)
    }

    @Test
    fun verifyPopOnRootReturnsCannotPopRoot() = runTest {
        val router = DefaultRouter(rootDestination)

        val result = router.execute(NavCommand.Pop)

        assertIs<NavResult.Rejected.CannotPopRoot>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("splash", router.currentState.activeEntry.destination.route)
    }

    @Test
    fun verifyReplaceUpdatesTop() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        val loginDest = NavDestination.create("login")

        val result = router.execute(NavCommand.Replace(loginDest))

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("login", router.currentState.activeEntry.destination.route)
        assertEquals("test_entry_2", router.currentState.activeEntry.entryId)
    }

    @Test
    fun verifyResetToClearsHistory() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        router.execute(NavCommand.Push(NavDestination.create("home")))
        router.execute(NavCommand.Push(NavDestination.create("details")))

        val mainDest = NavDestination.create("main")
        val result = router.execute(NavCommand.ResetTo(mainDest))

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("main", router.currentState.activeEntry.destination.route)
        assertEquals("test_entry_4", router.currentState.activeEntry.entryId)
    }

    @Test
    fun verifyPopToByRouteLastMatch() = runTest {
        val router = DefaultRouter(rootDestination)
        router.execute(NavCommand.Push(NavDestination.create("home")))
        router.execute(NavCommand.Push(NavDestination.create("details")))
        router.execute(NavCommand.Push(NavDestination.create("home")))
        router.execute(NavCommand.Push(NavDestination.create("settings")))

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("home"), inclusive = false))

        assertIs<NavResult.Executed>(result)
        assertEquals(4, router.currentState.stack.size)
        assertEquals("home", router.currentState.activeEntry.destination.route)
    }

    @Test
    fun verifyPopToByEntryId() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        router.execute(NavCommand.Push(NavDestination.create("home"))) // test_entry_2
        router.execute(NavCommand.Push(NavDestination.create("details"))) // test_entry_3

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByEntryId("test_entry_2"), inclusive = false))

        assertIs<NavResult.Executed>(result)
        assertEquals(2, router.currentState.stack.size)
        assertEquals("test_entry_2", router.currentState.activeEntry.entryId)
    }

    @Test
    fun verifyPopToInclusive() = runTest {
        val idGen = SequentialIdGenerator()
        val router = DefaultRouter(rootDestination, idGen)
        router.execute(NavCommand.Push(NavDestination.create("home"))) // test_entry_2
        router.execute(NavCommand.Push(NavDestination.create("details"))) // test_entry_3

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("home"), inclusive = true))

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.currentState.stack.size)
        assertEquals("splash", router.currentState.activeEntry.destination.route)
    }

    @Test
    fun verifyPopToMissingTargetReturnsTargetNotFound() = runTest {
        val router = DefaultRouter(rootDestination)

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("non_existent")))

        assertIs<NavResult.Rejected.TargetNotFound>(result)
        assertEquals(1, router.currentState.stack.size)
    }

    @Test
    fun verifyBlankRouteRejected() = runTest {
        val router = DefaultRouter(rootDestination)
        val blankDest = NavDestination.create("")

        val result = router.execute(NavCommand.Push(blankDest))

        assertIs<NavResult.Rejected.InvalidRoute>(result)
        assertEquals(1, router.currentState.stack.size)
    }

    @Test
    fun verifyWhitespaceRouteRejected() = runTest {
        val router = DefaultRouter(rootDestination)
        val wsDest = NavDestination.create("   ")

        val result = router.execute(NavCommand.Push(wsDest))

        assertIs<NavResult.Rejected.InvalidRoute>(result)
        assertEquals(1, router.currentState.stack.size)
    }

    @Test
    fun verifyParameterMapImmutability() = runTest {
        val mutableParams = mutableMapOf("id" to "BK-100")
        val dest = NavDestination.create("booking", mutableParams)

        mutableParams["id"] = "BK-200"

        assertEquals("BK-100", dest.params["id"])
    }

    @Test
    fun verifyMutexSerializesConcurrentCommands() = runTest {
        val router = DefaultRouter(rootDestination)
        val count = 20

        val deferreds = List(count) { i ->
            async(Dispatchers.Default) {
                router.execute(NavCommand.Push(NavDestination.create("route_$i")))
            }
        }
        deferreds.awaitAll()

        assertEquals(21, router.currentState.stack.size)
    }

    @Test
    fun verifyObservabilityEventsEmitted() = runTest {
        val logger = TestLogger()
        val router = DefaultRouter(rootDestination, logger = logger)

        router.execute(NavCommand.Push(NavDestination.create("home")))

        assertTrue(logger.events.any { it.event == "ROUTER_INITIALIZED" && it.category == LogCategory.NAVIGATION })
        assertTrue(logger.events.any { it.event == "PUSH_EXECUTED" && it.category == LogCategory.NAVIGATION })
    }
}
