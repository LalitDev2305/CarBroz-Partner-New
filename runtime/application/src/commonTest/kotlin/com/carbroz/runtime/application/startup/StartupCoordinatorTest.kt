package com.carbroz.runtime.application.startup

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun `startup stops after first controlled failure`() = kotlinx.coroutines.test.runTest {
        val calls = mutableListOf<String>()
        val failure = StartupFailure(code = "SESSION_RESTORE_FAILED", recoverable = true)
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
}
