package com.carbroz.data.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
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
    fun healthCheckReportsHealthyForUsableDatabase() = runTest {
        val database = createTestDatabase()

        try {
            val health = DatabaseHealthCheck(database).check()
            assertTrue(health is DatabaseHealth.Healthy)
        } finally {
            database.close()
        }
    }

    private fun createTestDatabase(): CarBrozDatabase =
        Room.inMemoryDatabaseBuilder<CarBrozDatabase>()
            .setDriver(BundledSQLiteDriver())
            .build()
}
