package com.carbroz.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CarBrozDatabaseRuntimeTest {
    @Test
    fun metadataDaoPersistsUpsertsAndRemovesValues() = runTest {
        val database = createTestDatabase()

        try {
            val dao = database.metadataDao()
            assertNull(dao.get("bootstrap.version"))

            dao.upsert(DatabaseMetadataEntity("bootstrap.version", "1"))
            assertEquals("1", dao.get("bootstrap.version")?.value)

            dao.upsert(DatabaseMetadataEntity("bootstrap.version", "2"))
            assertEquals("2", dao.get("bootstrap.version")?.value)

            dao.remove("bootstrap.version")
            assertNull(dao.get("bootstrap.version"))
        } finally {
            database.close()
        }
    }

    @Test
    fun syncOutboxDaoPersistsOrdersAndRemovesOperations() = runTest {
        val database = createTestDatabase()

        try {
            val dao = database.syncOutboxDao()
            assertEquals(0, dao.observePendingCount().first())

            dao.upsert(outboxEntity("b", createdAt = 20L, nextAttemptAt = 100L))
            dao.upsert(outboxEntity("a", createdAt = 10L, nextAttemptAt = 100L))
            dao.upsert(outboxEntity("future", createdAt = 5L, nextAttemptAt = 1_000L))

            assertTrue(dao.contains("a"))
            assertEquals(listOf("a", "b"), dao.ready(100L, 10).map { it.operationId })

            dao.remove("a")
            assertEquals(false, dao.contains("a"))
        } finally {
            database.close()
        }
    }

    @Test
    fun healthCheckReportsHealthyForUsableDatabase() = runTest {
        val database = createTestDatabase()

        try {
            val health = RoomDatabaseHealthCheck(database).check()
            assertTrue(health is DatabaseHealth.Healthy)
        } finally {
            database.close()
        }
    }

    private fun outboxEntity(
        id: String,
        createdAt: Long,
        nextAttemptAt: Long,
    ) = SyncOutboxEntity(
        operationId = id,
        method = "POST",
        endpoint = "/sync",
        payloadJson = null,
        headersJson = "{}",
        authentication = "SESSION",
        idempotencyKey = id,
        timeoutMillis = 15_000L,
        maxAttempts = 1,
        initialRetryDelayMillis = 250L,
        maxRetryDelayMillis = 4_000L,
        backoffMultiplier = 2.0,
        createdAtEpochMilliseconds = createdAt,
        attemptCount = 0,
        nextAttemptAtEpochMilliseconds = nextAttemptAt,
    )

    private fun createTestDatabase(): CarBrozDatabase =
        Room.inMemoryDatabaseBuilder<CarBrozDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
}
