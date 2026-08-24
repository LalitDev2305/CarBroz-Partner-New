package com.carbroz.data.sync

import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DefaultSyncCoordinator(
    private val outbox: OutboxStore,
    private val network: NetworkDataSource,
    private val clock: Clock,
    private val conflictResolver: SyncConflictResolver,
    private val retryPolicy: SyncRetryPolicy = SyncRetryPolicy(),
    private val batchSize: Int = 50,
) : SyncCoordinator {
    private val mutex = Mutex()

    init { require(batchSize > 0) }

    override suspend fun synchronize(trigger: SyncTrigger): SyncReport = mutex.withLock {
        val now = clock.nowEpochMilliseconds()
        val operations = outbox.ready(now, batchSize)
        var succeeded = 0
        var deferred = 0
        var conflicts = 0
        var permanentFailures = 0

        for (operation in operations) {
            when (val result = network.execute(operation.request)) {
                is NetworkResult.Success -> {
                    outbox.remove(operation.id)
                    succeeded++
                }
                is NetworkResult.Failure -> when (val failure = result.error) {
                    NetworkFailure.Offline,
                    NetworkFailure.Timeout,
                    NetworkFailure.Transport -> {
                        defer(operation, now)
                        deferred++
                    }
                    is NetworkFailure.Http -> when {
                        failure.statusCode == 409 || failure.statusCode == 412 -> {
                            conflicts++
                            when (conflictResolver.resolve(operation, failure.statusCode)) {
                                ConflictResolution.DiscardQueued -> outbox.remove(operation.id)
                                ConflictResolution.KeepQueued -> defer(operation, now)
                            }
                        }
                        failure.statusCode == 408 || failure.statusCode == 429 || failure.statusCode >= 500 -> {
                            defer(operation, now)
                            deferred++
                        }
                        else -> {
                            outbox.remove(operation.id)
                            permanentFailures++
                        }
                    }
                    is NetworkFailure.InvalidRequest -> {
                        outbox.remove(operation.id)
                        permanentFailures++
                    }
                }
            }
        }

        SyncReport(
            attempted = operations.size,
            succeeded = succeeded,
            deferred = deferred,
            conflicts = conflicts,
            permanentFailures = permanentFailures,
        )
    }

    private suspend fun defer(operation: OutboxOperation, now: Long) {
        val nextAttempt = operation.attemptCount + 1
        outbox.replace(
            operation.copy(
                attemptCount = nextAttempt,
                nextAttemptAtEpochMilliseconds = now + retryPolicy.delayMilliseconds(nextAttempt),
            ),
        )
    }
}

data class SyncRetryPolicy(
    val initialDelayMilliseconds: Long = 1_000,
    val maximumDelayMilliseconds: Long = 60_000,
) {
    init {
        require(initialDelayMilliseconds > 0)
        require(maximumDelayMilliseconds >= initialDelayMilliseconds)
    }

    fun delayMilliseconds(attempt: Int): Long {
        require(attempt > 0)
        var delay = initialDelayMilliseconds
        repeat((attempt - 1).coerceAtMost(30)) {
            delay = (delay * 2).coerceAtMost(maximumDelayMilliseconds)
        }
        return delay
    }
}
