package com.carbroz.partner.core.navigation

import com.carbroz.partner.core.observability.logger.BoundLogger
import com.carbroz.partner.core.observability.logger.StructuredLogger
import com.carbroz.partner.core.observability.model.LogAttribute
import com.carbroz.partner.core.observability.model.LogCategory
import com.carbroz.partner.core.observability.model.LogEvent
import com.carbroz.partner.core.observability.model.LogLevel
import com.carbroz.partner.core.observability.model.TraceContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class RouterConcurrencyTest {

    private class NoOpLogger : StructuredLogger {
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
                ) {}
            }
        }
    }

    @Test
    fun verifyConcurrentPushOperationsAreSerializedAndUnique() = runTest {
        val testScope = TestScope(UnconfinedTestDispatcher(testScheduler))
        val router = createRouter(NavDestination.create("root"), NoOpLogger())
        val concurrentCount = 50

        val jobs = List(concurrentCount) { index ->
            testScope.launch {
                router.execute(NavCommand.Push(NavDestination.create("screen_$index")))
            }
        }

        jobs.joinAll()

        // 1 root entry + 50 pushed entries = 51 entries
        assertEquals(concurrentCount + 1, router.state.value.size)

        val entryIds = router.state.value.entries.map { it.entryId }
        assertEquals(entryIds.size, entryIds.toSet().size, "All generated entry IDs must be strictly unique")
    }
}
