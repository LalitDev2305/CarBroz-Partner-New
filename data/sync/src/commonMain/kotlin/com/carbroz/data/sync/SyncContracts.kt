package com.carbroz.data.sync

import com.carbroz.data.network.NetworkRequest
import kotlinx.coroutines.flow.Flow

@JvmInline
value class OutboxOperationId(val value: String) {
    init { require(value.isNotBlank()) }
}

data class OutboxOperation(
    val id: OutboxOperationId,
    val request: NetworkRequest,
    val createdAtEpochMilliseconds: Long,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMilliseconds: Long = createdAtEpochMilliseconds,
) {
    init {
        require(attemptCount >= 0)
        require(request.idempotencyKey != null) { "Queued mutations require an idempotency key" }
    }
}

interface OutboxStore {
    fun observePendingCount(): Flow<Int>
    suspend fun enqueue(operation: OutboxOperation): Boolean
    suspend fun ready(nowEpochMilliseconds: Long, limit: Int): List<OutboxOperation>
    suspend fun replace(operation: OutboxOperation)
    suspend fun remove(id: OutboxOperationId)
}

enum class SyncTrigger { MANUAL, CONNECTIVITY_RESTORED, FOREGROUND, RETRY }

data class SyncReport(
    val attempted: Int,
    val succeeded: Int,
    val deferred: Int,
    val conflicts: Int,
    val permanentFailures: Int,
)

sealed interface ConflictResolution {
    data object KeepQueued : ConflictResolution
    data object DiscardQueued : ConflictResolution
}

fun interface SyncConflictResolver {
    suspend fun resolve(operation: OutboxOperation, statusCode: Int): ConflictResolution
}

fun interface SyncCoordinator {
    suspend fun synchronize(trigger: SyncTrigger = SyncTrigger.MANUAL): SyncReport
}
