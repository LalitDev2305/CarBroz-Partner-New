package com.carbroz.runtime.application

import com.carbroz.runtime.application.startup.StartupCoordinator
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationRuntimeTest {
    @Test
    fun `start transitions idle to ready`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("configuration") {
                    executions += 1
                    StartupTaskResult.Success
                }),
            ),
        )

        assertEquals(ApplicationRuntimeState.Idle, runtime.state.value)
        assertEquals(ApplicationRuntimeState.Ready(1u), runtime.start())
        assertEquals(ApplicationRuntimeState.Ready(1u), runtime.state.value)
        assertEquals(1, executions)
    }

    @Test
    fun `repeated start does not execute bootstrap twice`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("configuration") {
                    executions += 1
                    StartupTaskResult.Success
                }),
            ),
        )

        runtime.start()
        runtime.start()

        assertEquals(1, executions)
    }

    @Test
    fun `recoverable failure can retry and increments attempt`() = runTest {
        var executions = 0
        val recoverable = StartupFailure.Expected("TEMPORARY", recoverable = true)
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("configuration") {
                    executions += 1
                    if (executions == 1) StartupTaskResult.Failure(recoverable) else StartupTaskResult.Success
                }),
            ),
        )

        assertEquals(
            ApplicationRuntimeState.Failed("configuration", recoverable, 1u),
            runtime.start(),
        )
        assertEquals(ApplicationRuntimeState.Ready(2u), runtime.retry())
        assertEquals(2, executions)
    }

    @Test
    fun `non recoverable failure is not retried`() = runTest {
        var executions = 0
        val failure = StartupFailure.Unexpected
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("configuration") {
                    executions += 1
                    StartupTaskResult.Failure(failure)
                }),
            ),
        )

        val failed = runtime.start()
        assertEquals(failed, runtime.retry())
        assertEquals(1, executions)
    }

    @Test
    fun `concurrent start callers share one bootstrap execution`() = runTest {
        var executions = 0
        val runtime = DefaultApplicationRuntime(
            StartupCoordinator(
                listOf(task("configuration") {
                    executions += 1
                    StartupTaskResult.Success
                }),
            ),
        )

        val first = async { runtime.start() }
        val second = async { runtime.start() }

        assertEquals(ApplicationRuntimeState.Ready(1u), first.await())
        assertEquals(ApplicationRuntimeState.Ready(1u), second.await())
        assertEquals(1, executions)
    }

    private fun task(
        id: String,
        execute: suspend () -> StartupTaskResult,
    ): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult = execute()
    }
}
