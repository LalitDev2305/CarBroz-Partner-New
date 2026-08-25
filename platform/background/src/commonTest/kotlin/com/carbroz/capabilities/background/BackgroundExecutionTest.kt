package com.carbroz.platform.background

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class BackgroundExecutionTest {
    @Test
    fun taskIdRejectsBlankAndOversizedValues() {
        assertFailsWith<IllegalArgumentException> { BackgroundTaskId("") }
        assertFailsWith<IllegalArgumentException> {
            BackgroundTaskId("x".repeat(BackgroundTaskId.MAX_LENGTH + 1))
        }
    }

    @Test
    fun requestRejectsNegativeDelayAndOversizedInput() {
        assertFailsWith<IllegalArgumentException> {
            BackgroundTaskRequest(
                id = BackgroundTaskId("refresh"),
                kind = BackgroundTaskKind.REFRESH,
                earliestStartDelayMillis = -1,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            BackgroundTaskRequest(
                id = BackgroundTaskId("refresh"),
                kind = BackgroundTaskKind.REFRESH,
                input = mapOf("key" to "x".repeat(BackgroundTaskRequest.MAX_INPUT_VALUE_LENGTH + 1)),
            )
        }
    }

    @Test
    fun continuousRequestRejectsBlankAndOversizedUserVisibleText() {
        val id = BackgroundTaskId("tracking")
        assertFailsWith<IllegalArgumentException> {
            ContinuousExecutionRequest(id, "", "active")
        }
        assertFailsWith<IllegalArgumentException> {
            ContinuousExecutionRequest(
                id,
                "x".repeat(ContinuousExecutionRequest.MAX_TITLE_LENGTH + 1),
                "active",
            )
        }
        assertFailsWith<IllegalArgumentException> {
            ContinuousExecutionRequest(
                id,
                "Tracking",
                "x".repeat(ContinuousExecutionRequest.MAX_DESCRIPTION_LENGTH + 1),
            )
        }
    }

    @Test
    fun registryRejectsDuplicateHandlers() {
        val first = handler("sync") { BackgroundExecutionResult.Success }
        val second = handler("sync") { BackgroundExecutionResult.Retry }
        assertFailsWith<IllegalArgumentException> {
            BackgroundTaskHandlerRegistry(listOf(first, second))
        }
    }

    @Test
    fun runnerDispatchesAndNormalizesUnexpectedFailure() = runTest {
        val success = handler("success") { BackgroundExecutionResult.Success }
        val failure = handler("failure") { error("boom") }
        val runner = BackgroundTaskRunner(BackgroundTaskHandlerRegistry(listOf(success, failure)))

        assertEquals(
            BackgroundExecutionResult.Success,
            runner.run(BackgroundTaskId("success"), mapOf("a" to "b")),
        )
        assertIs<BackgroundExecutionResult.Failure>(runner.run(BackgroundTaskId("failure"), emptyMap()))
        assertIs<BackgroundExecutionResult.Failure>(runner.run(BackgroundTaskId("missing"), emptyMap()))
    }

    @Test
    fun runnerPropagatesCancellation() = runTest {
        val cancelling = handler("cancel") { throw CancellationException("cancel") }
        val runner = BackgroundTaskRunner(BackgroundTaskHandlerRegistry(listOf(cancelling)))
        assertFailsWith<CancellationException> { runner.run(BackgroundTaskId("cancel"), emptyMap()) }
    }

    private fun handler(
        id: String,
        execute: suspend (Map<String, String>) -> BackgroundExecutionResult,
    ) = object : BackgroundTaskHandler {
        override val id = BackgroundTaskId(id)
        override suspend fun execute(input: Map<String, String>): BackgroundExecutionResult = execute(input)
    }
}
