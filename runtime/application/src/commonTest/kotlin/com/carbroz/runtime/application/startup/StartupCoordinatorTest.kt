package com.carbroz.runtime.application.startup

import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StartupCoordinatorTest {
    @Test
    fun `tasks execute in declared order`() = kotlinx.coroutines.test.runTest {
        val calls = mutableListOf<String>()
        val coordinator = StartupCoordinator(
            listOf(
                task("configuration", calls),
                task("observability", calls),
                task("session", calls),
            ),
        )

        assertEquals(StartupResult.Ready, coordinator.run())
        assertEquals(listOf("configuration", "observability", "session"), calls)
    }

    @Test
    fun `startup stops after first expected failure`() = kotlinx.coroutines.test.runTest {
        val calls = mutableListOf<String>()
        val failure = StartupFailure.Expected(
            code = "SESSION_RESTORE_FAILED",
            recoverable = true,
        )
        val coordinator = StartupCoordinator(
            listOf(
                task("configuration", calls),
                task("session", calls, StartupTaskResult.Failure(failure)),
                task("database", calls),
            ),
        )

        assertEquals(StartupResult.Failed("session", failure), coordinator.run())
        assertEquals(listOf("configuration", "session"), calls)
    }

    @Test
    fun `unexpected task exception becomes stable failure and stops startup`() = kotlinx.coroutines.test.runTest {
        val calls = mutableListOf<String>()
        val coordinator = StartupCoordinator(
            listOf(
                task("configuration", calls),
                throwingTask("session", calls, IllegalStateException("secret detail")),
                task("database", calls),
            ),
        )

        assertEquals(
            StartupResult.Failed("session", StartupFailure.Unexpected),
            coordinator.run(),
        )
        assertEquals(listOf("configuration", "session"), calls)
    }

    @Test
    fun `cancellation is never converted into startup failure`() = kotlinx.coroutines.test.runTest {
        val coordinator = StartupCoordinator(
            listOf(
                throwingTask("session", mutableListOf(), CancellationException("cancelled")),
            ),
        )

        assertFailsWith<CancellationException> { coordinator.run() }
    }

    @Test
    fun `duplicate task ids are rejected before startup`() {
        assertFailsWith<IllegalArgumentException> {
            StartupCoordinator(
                listOf(
                    task("configuration", mutableListOf()),
                    task("configuration", mutableListOf()),
                ),
            )
        }
    }

    private fun task(
        id: String,
        calls: MutableList<String>,
        result: StartupTaskResult = StartupTaskResult.Success,
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
}
