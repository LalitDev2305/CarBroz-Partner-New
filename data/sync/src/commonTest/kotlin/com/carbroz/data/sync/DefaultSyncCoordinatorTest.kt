package com.carbroz.data.sync

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResponse
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DefaultSyncCoordinatorTest {
    @Test
    fun successfulMutationIsRemoved() = runTest {
        val operation = operation()
        val store = FakeOutboxStore(operation)
        val coordinator = coordinator(store, NetworkResult.Success(NetworkResponse(200)))

        val report = coordinator.synchronize(SyncTrigger.MANUAL)

        assertEquals(1, report.succeeded)
        assertEquals(emptyList(), store.operations)
    }

    @Test
    fun transientFailureIsDeferredWithBackoff() = runTest {
        val operation = operation()
        val store = FakeOutboxStore(operation)
        val coordinator = coordinator(store, NetworkResult.Failure(NetworkFailure.Offline))

        val report = coordinator.synchronize(SyncTrigger.CONNECTIVITY_RESTORED)

        assertEquals(1, report.deferred)
        assertEquals(1, store.operations.single().attemptCount)
        assertEquals(2_000L, store.operations.single().nextAttemptAtEpochMilliseconds)
    }

    @Test
    fun conflictUsesResolverWithoutGuessingProductSemantics() = runTest {
        val operation = operation()
        val store = FakeOutboxStore(operation)
        val coordinator = DefaultSyncCoordinator(
            outbox = store,
            network = NetworkDataSource { NetworkResult.Failure(NetworkFailure.Http(409)) },
            clock = Clock { 1_000L },
            conflictResolver = SyncConflictResolver { _, _ -> ConflictResolution.DiscardQueued },
        )

        val report = coordinator.synchronize(SyncTrigger.MANUAL)

        assertEquals(1, report.conflicts)
        assertEquals(emptyList(), store.operations)
    }

    @Test
    fun outboxRejectsReadsAndMissingIdempotency() {
        assertFailsWith<IllegalArgumentException> {
            operation(method = NetworkMethod.GET)
        }
        assertFailsWith<IllegalArgumentException> {
            operation(idempotencyKey = null)
        }
    }

    private fun coordinator(store: FakeOutboxStore, result: NetworkResult) = DefaultSyncCoordinator(
        outbox = store,
        network = NetworkDataSource { result },
        clock = Clock { 1_000L },
        conflictResolver = KeepQueuedSyncConflictResolver,
    )

    private fun operation(
        method: NetworkMethod = NetworkMethod.POST,
        idempotencyKey: String? = "op-1",
    ) = OutboxOperation(
        id = OutboxOperationId("op-1"),
        request = NetworkRequest(
            method = method,
            endpoint = NetworkEndpoint("/sync"),
            idempotencyKey = idempotencyKey,
            authentication = NetworkAuthentication.SESSION,
        ),
        createdAtEpochMilliseconds = 500L,
    )

    private class FakeOutboxStore(vararg initial: OutboxOperation) : OutboxStore {
        val operations = initial.toMutableList()

        override fun observePendingCount(): Flow<Int> = flowOf(operations.size)
        override suspend fun enqueue(operation: OutboxOperation): Boolean {
            if (operations.any { it.id == operation.id }) return false
            operations += operation
            return true
        }
        override suspend fun ready(nowEpochMilliseconds: Long, limit: Int): List<OutboxOperation> =
            operations.filter { it.nextAttemptAtEpochMilliseconds <= nowEpochMilliseconds }.take(limit)
        override suspend fun replace(operation: OutboxOperation) {
            val index = operations.indexOfFirst { it.id == operation.id }
            require(index >= 0)
            operations[index] = operation
        }
        override suspend fun remove(id: OutboxOperationId) {
            operations.removeAll { it.id == id }
        }
    }
}
