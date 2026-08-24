package com.carbroz.data.sync

import com.carbroz.data.database.CarBrozDatabase
import com.carbroz.data.database.SyncOutboxEntity
import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkExecutionPolicy
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RoomOutboxStore(
    database: CarBrozDatabase,
    private val json: Json = Json,
) : OutboxStore {
    private val dao = database.syncOutboxDao()

    override fun observePendingCount(): Flow<Int> = dao.observePendingCount()

    override suspend fun enqueue(operation: OutboxOperation): Boolean {
        if (dao.contains(operation.id.value)) return false
        dao.upsert(operation.toEntity())
        return true
    }

    override suspend fun ready(nowEpochMilliseconds: Long, limit: Int): List<OutboxOperation> {
        require(limit > 0)
        return dao.ready(nowEpochMilliseconds, limit).map { it.toOperation() }
    }

    override suspend fun replace(operation: OutboxOperation) {
        require(dao.contains(operation.id.value)) { "Cannot replace an operation that is not queued" }
        dao.upsert(operation.toEntity())
    }

    override suspend fun remove(id: OutboxOperationId) {
        dao.remove(id.value)
    }

    private fun OutboxOperation.toEntity(): SyncOutboxEntity {
        val policy = request.executionPolicy
        return SyncOutboxEntity(
            operationId = id.value,
            method = request.method.name,
            endpoint = request.endpoint.value,
            payloadJson = request.payload?.toString(),
            headersJson = buildJsonObject {
                request.headers.forEach { (key, value) -> put(key, JsonPrimitive(value)) }
            }.toString(),
            authentication = request.authentication.name,
            idempotencyKey = requireNotNull(request.idempotencyKey),
            timeoutMillis = policy.timeoutMillis,
            maxAttempts = policy.maxAttempts,
            initialRetryDelayMillis = policy.initialRetryDelayMillis,
            maxRetryDelayMillis = policy.maxRetryDelayMillis,
            backoffMultiplier = policy.backoffMultiplier,
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            attemptCount = attemptCount,
            nextAttemptAtEpochMilliseconds = nextAttemptAtEpochMilliseconds,
        )
    }

    private fun SyncOutboxEntity.toOperation(): OutboxOperation {
        val headers = json.parseToJsonElement(headersJson).jsonObject
            .mapValues { (_, value) -> value.jsonPrimitive.content }
        val payload = payloadJson?.let { json.parseToJsonElement(it).jsonObject }

        return OutboxOperation(
            id = OutboxOperationId(operationId),
            request = NetworkRequest(
                method = NetworkMethod.valueOf(method),
                endpoint = NetworkEndpoint(endpoint),
                payload = payload,
                headers = headers,
                executionPolicy = NetworkExecutionPolicy(
                    timeoutMillis = timeoutMillis,
                    maxAttempts = maxAttempts,
                    initialRetryDelayMillis = initialRetryDelayMillis,
                    maxRetryDelayMillis = maxRetryDelayMillis,
                    backoffMultiplier = backoffMultiplier,
                ),
                idempotencyKey = idempotencyKey,
                authentication = NetworkAuthentication.valueOf(authentication),
                cachePolicy = NetworkCachePolicy.NetworkOnly,
            ),
            createdAtEpochMilliseconds = createdAtEpochMilliseconds,
            attemptCount = attemptCount,
            nextAttemptAtEpochMilliseconds = nextAttemptAtEpochMilliseconds,
        )
    }
}
