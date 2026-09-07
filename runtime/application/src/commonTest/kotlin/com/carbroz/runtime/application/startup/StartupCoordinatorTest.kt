package com.carbroz.runtime.application.startup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class StartupCoordinatorTest {
    @Test
    fun `continue tasks execute in order until final resolution`() = runTest {
        val calls = mutableListOf<String>()
        val resolution = StartupResolution.Ready(TestPayload)
        val coordinator = StartupCoordinator(
            listOf(
                task("configuration", calls, StartupTaskResult.Continue),
                task("session", calls, StartupTaskResult.Continue),
                task("bootstrap", calls, StartupTaskResult.Resolved(resolution)),
                task("never", calls, StartupTaskResult.Continue),
            ),
        )

        assertEquals(StartupResult.Resolved(resolution), coordinator.run())
        assertEquals(listOf("configuration", "session", "bootstrap"), calls)
    }

    @Test
    fun `blocked resolution is successful final resolution not failure`() = runTest {
        val blocker = StartupBlocker.Maintenance("Maintenance", "Try later")
        val result = StartupCoordinator(
            listOf(task("bootstrap", mutableListOf(), StartupTaskResult.Resolved(StartupResolution.Blocked(blocker)))),
        ).run()

        val resolved = assertIs<StartupResult.Resolved>(result)
        assertEquals(StartupResolution.Blocked(blocker), resolved.resolution)
    }

    @Test
    fun `startup stops after first expected failure`() = runTest {
        val calls = mutableListOf<String>()
        val failure = StartupFailure.Expected("session_restore_failed", recoverable = true)
        val coordinator = StartupCoordinator(
            listOf(
                task("configuration", calls, StartupTaskResult.Continue),
                task("session", calls, StartupTaskResult.Failure(failure)),
                task("bootstrap", calls, StartupTaskResult.Resolved(StartupResolution.Ready(TestPayload))),
            ),
        )

        assertEquals(StartupResult.Failed("session", failure), coordinator.run())
        assertEquals(listOf("configuration", "session"), calls)
    }

    @Test
    fun `all continue tasks fail closed without a final resolution`() = runTest {
        val result = StartupCoordinator(
            listOf(task("session", mutableListOf(), StartupTaskResult.Continue)),
        ).run()

        val failed = assertIs<StartupResult.Failed>(result)
        assertEquals("startup.coordinator", failed.taskId)
        assertEquals(
            StartupFailure.Expected("startup_missing_resolution", recoverable = false),
            failed.failure,
        )
    }

    @Test
    fun `unexpected task exception becomes stable failure and stops startup`() = runTest {
        val calls = mutableListOf<String>()
        val coordinator = StartupCoordinator(
            listOf(
                task("session", calls, StartupTaskResult.Continue),
                throwingTask("bootstrap", calls, IllegalStateException("secret detail")),
            ),
        )

        assertEquals(StartupResult.Failed("bootstrap", StartupFailure.Unexpected), coordinator.run())
        assertEquals(listOf("session", "bootstrap"), calls)
    }

    @Test
    fun `cancellation is never converted into startup failure`() = runTest {
        val coordinator = StartupCoordinator(
            listOf(throwingTask("session", mutableListOf(), CancellationException("cancelled"))),
        )

        assertFailsWith<CancellationException> { coordinator.run() }
    }

    @Test
    fun `duplicate task ids are rejected before startup`() {
        assertFailsWith<IllegalArgumentException> {
            StartupCoordinator(
                listOf(
                    task("configuration", mutableListOf(), StartupTaskResult.Continue),
                    task("configuration", mutableListOf(), StartupTaskResult.Continue),
                ),
            )
        }
    }

    private fun task(
        id: String,
        calls: MutableList<String>,
        result: StartupTaskResult,
    ): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult {
            calls += id
            return result
        }
    }

    private fun throwingTask(
        id: String,
        calls: MutableList<String>,
        throwable: Throwable,
    ): StartupTask = object : StartupTask {
        override val id: String = id
        override suspend fun execute(): StartupTaskResult {
            calls += id
            throw throwable
        }
    }

    private data object TestPayload : StartupPayload
}
