package com.carbroz.data.database

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.executeSQL
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Platform-owned source of a Room database builder for the canonical application database. */
fun interface CarBrozDatabaseBuilderProvider {
    fun builder(): RoomDatabase.Builder<CarBrozDatabase>
}

/** Composition-facing provider that hides Room builder mechanics from higher layers. */
fun interface CarBrozDatabaseProvider {
    fun get(): CarBrozDatabase
}

private val Migration1To2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.executeSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_outbox (
                operationId TEXT NOT NULL PRIMARY KEY,
                method TEXT NOT NULL,
                endpoint TEXT NOT NULL,
                payloadJson TEXT,
                headersJson TEXT NOT NULL,
                authentication TEXT NOT NULL,
                idempotencyKey TEXT NOT NULL,
                timeoutMillis INTEGER NOT NULL,
                maxAttempts INTEGER NOT NULL,
                initialRetryDelayMillis INTEGER NOT NULL,
                maxRetryDelayMillis INTEGER NOT NULL,
                backoffMultiplier REAL NOT NULL,
                createdAtEpochMilliseconds INTEGER NOT NULL,
                attemptCount INTEGER NOT NULL,
                nextAttemptAtEpochMilliseconds INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

/** Builds the canonical database with the shared production driver configuration. */
class CarBrozDatabaseFactory(
    private val builderProvider: CarBrozDatabaseBuilderProvider,
) : CarBrozDatabaseProvider {
    override fun get(): CarBrozDatabase = builderProvider.builder()
        .setDriver(BundledSQLiteDriver())
        .addMigrations(Migration1To2)
        .build()
}
