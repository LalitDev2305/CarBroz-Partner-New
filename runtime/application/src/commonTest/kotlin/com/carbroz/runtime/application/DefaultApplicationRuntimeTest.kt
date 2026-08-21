package com.carbroz.runtime.application

import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultApplicationRuntimeTest {
    @Test
    fun `successful start transitions runtime to ready once`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(listOf(task("configuration") { executions += 1; StartupTaskResult.Success })),
        )

        assertEquals(ApplicationRuntimeState.Ready(1u), runtime.start())
        assertEquals(ApplicationRuntimeState.Ready(1u), runtime.start())
        assertEquals(1, executions)
    }

    @Test
    fun `recoverable failure can retry and increment attempt`() = runTest {
        var executions = 0
        val failure = StartupFailure.Expected("TEMPORARY", recoverable = true)
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(
                    task("session") {
                        executions += 1
                        if (executions == 1) StartupTaskResult.Failure(failure) else StartupTaskResult.Success
                    },
                ),
            ),
        )

        assertEquals(ApplicationRuntimeState.Failed("session", failure, 1u), runtime.start())
        assertEquals(ApplicationRuntimeState.Ready(2u), runtime.retry())
        assertEquals(2, executions)
    }

    @Test
    fun `non recoverable failure cannot retry`() = runTest {
        var executions = 0
        val failure = StartupFailure.Unexpected
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(listOf(task("session") { executions += 1; StartupTaskResult.Failure(failure) })),
        )

        val failed = runtime.start()
        assertEquals(failed, runtime.retry())
        assertEquals(1, executions)
    }

    @Test
    fun `concurrent start callers cannot duplicate bootstrap`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(listOf(task("configuration") { executions += 1; StartupTaskResult.Success })),
        )

        val results = List(8) { async { runtime.start() } }.awaitAll()

        assertEquals(List(8) { ApplicationRuntimeState.Ready(1u) }, results)
        assertEquals(1, executions)
    }

    private fun task(id: String, execute: suspend () -> StartupTaskResult): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult = execute()
    }
}
