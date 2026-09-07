package com.carbroz.runtime.application

import com.carbroz.runtime.application.startup.StartupBlocker
import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupPayload
import com.carbroz.runtime.application.startup.StartupResolution
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class DefaultApplicationRuntimeTest {
    @Test
    fun `successful start publishes one ready result and is idempotent`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("bootstrap") { executions += 1; readyResult() }),
            ),
        )

        val first = assertIs<ApplicationRuntimeState.Ready>(runtime.start())
        val second = runtime.start()
        assertEquals(1u, first.attempt)
        assertEquals(first, second)
        assertEquals(1, executions)
    }

    @Test
    fun `recoverable failure can retry and increment attempt`() = runTest {
        var executions = 0
        val failure = StartupFailure.Expected("temporary", recoverable = true)
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(
                    task("bootstrap") {
                        executions += 1
                        if (executions == 1) StartupTaskResult.Failure(failure) else readyResult()
                    },
                ),
            ),
        )

        assertEquals(ApplicationRuntimeState.Failed("bootstrap", failure, 1u), runtime.start())
        assertEquals(2u, assertIs<ApplicationRuntimeState.Ready>(runtime.retry()).attempt)
        assertEquals(2, executions)
    }

    @Test
    fun `retryable maintenance can retry but required update cannot`() = runTest {
        var maintenanceExecutions = 0
        val maintenance = StartupBlocker.Maintenance("Maintenance", "Try later", retryable = true)
        val maintenanceRuntime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(
                    task("bootstrap") {
                        maintenanceExecutions += 1
                        if (maintenanceExecutions == 1) {
                            StartupTaskResult.Resolved(StartupResolution.Blocked(maintenance))
                        } else {
                            readyResult()
                        }
                    },
                ),
            ),
        )

        assertIs<ApplicationRuntimeState.Blocked>(maintenanceRuntime.start())
        assertEquals(2u, assertIs<ApplicationRuntimeState.Ready>(maintenanceRuntime.retry()).attempt)

        var updateExecutions = 0
        val requiredUpdate = StartupBlocker.RequiredUpdate(null, null, "https://example.com/update")
        val updateRuntime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(
                    task("bootstrap") {
                        updateExecutions += 1
                        StartupTaskResult.Resolved(StartupResolution.Blocked(requiredUpdate))
                    },
                ),
            ),
        )
        val blocked = updateRuntime.start()
        assertEquals(blocked, updateRuntime.retry())
        assertEquals(1, updateExecutions)
    }

    @Test
    fun `concurrent start callers cannot duplicate bootstrap`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(listOf(task("bootstrap") { executions += 1; readyResult() })),
        )

        val results = List(8) { async { runtime.start() } }.awaitAll()

        assertEquals(1, executions)
        results.forEach { assertIs<ApplicationRuntimeState.Ready>(it) }
    }

    @Test
    fun `cancelled initial startup restores idle state`() = runTest {
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(listOf(task("bootstrap") { throw CancellationException("cancelled") })),
        )

        assertFailsWith<CancellationException> { runtime.start() }
        assertEquals(ApplicationRuntimeState.Idle, runtime.state.value)
    }

    @Test
    fun `cancelled retry restores previous failure state`() = runTest {
        var executions = 0
        val failure = StartupFailure.Expected("temporary", recoverable = true)
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(
                    task("bootstrap") {
                        executions += 1
                        if (executions == 1) StartupTaskResult.Failure(failure)
                        else throw CancellationException("cancelled")
                    },
                ),
            ),
        )

        val failed = runtime.start()
        assertFailsWith<CancellationException> { runtime.retry() }
        assertEquals(failed, runtime.state.value)
    }

    private fun readyResult(): StartupTaskResult = StartupTaskResult.Resolved(
        StartupResolution.Ready(TestPayload),
    )

    private fun task(id: String, execute: suspend () -> StartupTaskResult): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult = execute()
    }

    private data object TestPayload : StartupPayload
}
