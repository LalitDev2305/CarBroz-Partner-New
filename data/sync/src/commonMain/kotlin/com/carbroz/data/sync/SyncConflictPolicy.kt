package com.carbroz.data.sync

/**
 * Generic infrastructure cannot decide product conflict semantics safely.
 * Keep the operation queued until an owning feature supplies a domain-aware resolver.
 */
object KeepQueuedSyncConflictResolver : SyncConflictResolver {
    override suspend fun resolve(operation: OutboxOperation, statusCode: Int): ConflictResolution =
        ConflictResolution.KeepQueued
}
