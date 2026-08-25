package com.carbroz.capabilities.background

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopBackgroundExecutionTest {
    @Test
    fun unsupportedDesktopConstraintsAreRejectedRatherThanIgnored() = runTest {
        val scheduler = scheduler(this)

        val result = scheduler.schedule(
            request(
                constraints = BackgroundConstraints(network = NetworkRequirement.CONNECTED),
            ),
        )

        assertIs<BackgroundScheduleResult.Unsupported>(result)
        assertEquals(BackgroundTaskState.UNKNOWN, scheduler.state(ID))
    }

    @Test
    fun keepPolicyDoesNotDuplicateActiveWork() = runTest {
        val scheduler = scheduler(this)
        val delayed = request(delayMillis = 10_000)

        assertEquals(BackgroundScheduleResult.Scheduled, scheduler.schedule(delayed))
        assertEquals(BackgroundScheduleResult.AlreadyScheduled, scheduler.schedule(delayed))
        assertEquals(BackgroundTaskState.ENQUEUED, scheduler.state(ID))
    }

    @Test
    fun scheduledWorkRunsThroughCanonicalRunner() = runTest {
        val scheduler = scheduler(this)

        assertEquals(BackgroundScheduleResult.Scheduled, scheduler.schedule(request()))
        advanceUntilIdle()

        assertEquals(BackgroundTaskState.SUCCEEDED, scheduler.state(ID))
    }

    @Test
    fun cancellationAndContinuousExecutionStateAreExplicit() = runTest {
        val scheduler = scheduler(this)
        scheduler.schedule(request(delayMillis = 10_000))
        scheduler.cancel(ID)
        assertEquals(BackgroundTaskState.CANCELLED, scheduler.state(ID))

        val controller = DesktopContinuousExecutionController()
        val continuous = ContinuousExecutionRequest(ID, "Active operation", "CarBroz is performing ongoing work")
        assertEquals(ContinuousExecutionStartResult.Started, controller.start(continuous))
        assertEquals(ContinuousExecutionStartResult.AlreadyRunning, controller.start(continuous))
        assertEquals(ContinuousExecutionState.RUNNING, controller.state(ID))
        controller.stop(ID)
        assertEquals(ContinuousExecutionState.STOPPED, controller.state(ID))
    }

    private fun scheduler(scope: kotlinx.coroutines.CoroutineScope): DesktopBackgroundScheduler {
        val handler = object : BackgroundTaskHandler {
            override val id: BackgroundTaskId = ID
            override suspend fun execute(input: Map<String, String>): BackgroundExecutionResult =
                BackgroundExecutionResult.Success
        }
        val runner = BackgroundTaskRunner(BackgroundTaskHandlerRegistry(listOf(handler)))
        return DesktopBackgroundScheduler(runnerProvider = { runner }, scope = scope)
    }

    private fun request(
        delayMillis: Long = 0,
        constraints: BackgroundConstraints = BackgroundConstraints(),
    ) = BackgroundTaskRequest(
        id = ID,
        kind = BackgroundTaskKind.PROCESSING,
        earliestStartDelayMillis = delayMillis,
        constraints = constraints,
    )

    companion object {
        private val ID = BackgroundTaskId("desktop-test")
    }
}
