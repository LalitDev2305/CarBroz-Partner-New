package com.carbroz.partner.core.navigation

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RouterTest {

    private class TestLogger : StructuredLogger {
        val events = mutableListOf<LogEvent>()

        override fun isLevelEnabled(level: LogLevel): Boolean = true

        override fun withSource(sourceClass: String, defaultTraceContext: TraceContext?): BoundLogger {
            return object : BoundLogger {
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
                            timestampMs = 1000L,
                            level = level,
                            category = category,
                            sourceClass = sourceClass,
                            sourceFunction = sourceFunction,
                            event = event,
                            message = message,
                            traceContext = traceContext
                        )
                    )
                }
            }
        }
    }

    @Test
    fun verifyInitialRootState() = runTest {
        val logger = TestLogger()
        val rootDest = NavDestination.create("root")
        val router = createRouter(rootDest, logger)

        assertEquals(1, router.state.value.size)
        assertEquals("root", router.state.value.activeEntry.destination.route)
        assertEquals(LogCategory.NAVIGATION, logger.events[0].category)
        assertEquals("ROUTER_INITIALIZED", logger.events[0].event)
    }

    @Test
    fun verifyPushAppendsNewEntryWithUniqueEntryId() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        val dest2 = NavDestination.create("details")
        val result = router.execute(NavCommand.Push(dest2))

        assertIs<NavResult.Executed>(result)
        assertEquals("details", result.activeEntry.destination.route)
        assertEquals(2, router.state.value.size)
        assertNotEquals(router.state.value.entries[0].entryId, router.state.value.entries[1].entryId)
    }

    @Test
    fun verifyReplaceReplacesTopEntry() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        router.execute(NavCommand.Push(NavDestination.create("step1")))
        val result = router.execute(NavCommand.Replace(NavDestination.create("step2")))

        assertIs<NavResult.Executed>(result)
        assertEquals(2, router.state.value.size)
        assertEquals("step2", router.state.value.activeEntry.destination.route)
        assertEquals("root", router.state.value.entries[0].destination.route)
    }

    @Test
    fun verifyPopRemovesTopEntry() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        router.execute(NavCommand.Push(NavDestination.create("details")))
        val result = router.execute(NavCommand.Pop)

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.state.value.size)
        assertEquals("root", router.state.value.activeEntry.destination.route)
    }

    @Test
    fun verifyCannotPopRootRejectionPreservesStateAndLogsWarn() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        val result = router.execute(NavCommand.Pop)

        assertIs<NavResult.Rejected.CannotPopRoot>(result)
        assertEquals(1, router.state.value.size)
        assertEquals("root", router.state.value.activeEntry.destination.route)

        val warnLogs = logger.events.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnLogs.size)
        assertEquals("POP_REJECTED", warnLogs[0].event)
    }

    @Test
    fun verifyResetToReplacesEntireHistoryWithNewRoot() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        router.execute(NavCommand.Push(NavDestination.create("step1")))
        router.execute(NavCommand.Push(NavDestination.create("step2")))

        val result = router.execute(NavCommand.ResetTo(NavDestination.create("login")))

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.state.value.size)
        assertEquals("login", router.state.value.activeEntry.destination.route)
    }

    @Test
    fun verifyPopToByRouteSelectsLastMatchingRoute() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("home"), logger)

        router.execute(NavCommand.Push(NavDestination.create("feed")))
        router.execute(NavCommand.Push(NavDestination.create("home")))
        router.execute(NavCommand.Push(NavDestination.create("profile")))

        // PopTo route "home" exclusive -> keeps up to second "home"
        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("home"), inclusive = false))

        assertIs<NavResult.Executed>(result)
        assertEquals(3, router.state.value.size)
        assertEquals("home", router.state.value.activeEntry.destination.route)
    }

    @Test
    fun verifyPopToByEntryIdTargetsExactEntry() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        val pushResult = router.execute(NavCommand.Push(NavDestination.create("target_screen")))
        assertIs<NavResult.Executed>(pushResult)
        val targetEntryId = pushResult.activeEntry.entryId

        router.execute(NavCommand.Push(NavDestination.create("other_screen")))

        val popToResult = router.execute(NavCommand.PopTo(PopToTarget.ByEntryId(targetEntryId), inclusive = false))

        assertIs<NavResult.Executed>(popToResult)
        assertEquals(2, router.state.value.size)
        assertEquals(targetEntryId, router.state.value.activeEntry.entryId)
    }

    @Test
    fun verifyPopToInclusiveRemovesTargetUnlessRoot() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        router.execute(NavCommand.Push(NavDestination.create("step1")))
        router.execute(NavCommand.Push(NavDestination.create("step2")))

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("step1"), inclusive = true))

        assertIs<NavResult.Executed>(result)
        assertEquals(1, router.state.value.size)
        assertEquals("root", router.state.value.activeEntry.destination.route)
    }

    @Test
    fun verifyPopToInclusiveRootProtectionRejectsAndPreservesState() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        router.execute(NavCommand.Push(NavDestination.create("step1")))

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("root"), inclusive = true))

        assertIs<NavResult.Rejected.CannotPopRoot>(result)
        assertEquals(2, router.state.value.size)
    }

    @Test
    fun verifyTargetNotFoundRejectionPreservesStateAndLogsWarn() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root"), logger)

        val result = router.execute(NavCommand.PopTo(PopToTarget.ByRoute("missing"), inclusive = false))

        assertIs<NavResult.Rejected.TargetNotFound>(result)
        assertEquals(1, router.state.value.size)

        val warnLogs = logger.events.filter { it.level == LogLevel.WARN }
        assertEquals(1, warnLogs.size)
        assertEquals("POP_TO_REJECTED", warnLogs[0].event)
    }

    @Test
    fun verifyObservabilityLogsAreStructuralAndContainNoSecretsOrParams() = runTest {
        val logger = TestLogger()
        val router = createRouter(NavDestination.create("root", mapOf("secret" to "my_auth_token")), logger)

        router.execute(NavCommand.Push(NavDestination.create("screen2", mapOf("booking_id" to "bk_999"))))

        for (event in logger.events) {
            assertFalse(event.message.contains("my_auth_token"))
            assertFalse(event.message.contains("bk_999"))
            assertFalse(event.message.contains("root"))
            assertFalse(event.message.contains("screen2"))
        }
    }
}
