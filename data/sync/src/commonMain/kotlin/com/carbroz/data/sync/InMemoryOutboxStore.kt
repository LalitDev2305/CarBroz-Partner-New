package com.carbroz.data.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Deterministic process-local implementation useful where durable persistence is supplied later by composition. */
class InMemoryOutboxStore : OutboxStore {
    private val mutex = Mutex()
    private val operations = linkedMapOf<OutboxOperationId, OutboxOperation>()
    private val pendingCount = MutableStateFlow(0)

    override fun observePendingCount(): Flow<Int> = pendingCount.asStateFlow()

    override suspend fun enqueue(operation: OutboxOperation): Boolean = mutex.withLock {
        if (operations.containsKey(operation.id)) return@withLock false
        operations[operation.id] = operation
        pendingCount.value = operations.size
        true
    }

    override suspend fun ready(nowEpochMilliseconds: Long, limit: Int): List<OutboxOperation> = mutex.withLock {
        require(limit > 0)
        operations.values
            .asSequence()
            .filter { it.nextAttemptAtEpochMilliseconds <= nowEpochMilliseconds }
            .sortedWith(compareBy<OutboxOperation> { it.createdAtEpochMilliseconds }.thenBy { it.id.value })
            .take(limit)
            .toList()
    }

    override suspend fun replace(operation: OutboxOperation) = mutex.withLock {
        require(operations.containsKey(operation.id)) { "Cannot replace an operation that is not queued" }
        operations[operation.id] = operation
    }

    override suspend fun remove(id: OutboxOperationId) = mutex.withLock {
        operations.remove(id)
        pendingCount.value = operations.size
    }
}
