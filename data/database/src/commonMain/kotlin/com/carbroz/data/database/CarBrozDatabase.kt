package com.carbroz.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Canonical application database.
 *
 * Product/business entities remain absent until their owning repository contracts exist.
 * Infrastructure-owned metadata and sync outbox records are permitted because their ownership
 * and lifecycle are defined by foundation data contracts rather than product feature models.
 */
@Database(
    entities = [DatabaseMetadataEntity::class, SyncOutboxEntity::class],
    version = CarBrozDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@ConstructedBy(CarBrozDatabaseConstructor::class)
abstract class CarBrozDatabase : RoomDatabase() {
    abstract fun metadataDao(): DatabaseMetadataDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        const val SCHEMA_VERSION: Int = 2
        const val DATABASE_NAME: String = "carbroz.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object CarBrozDatabaseConstructor : RoomDatabaseConstructor<CarBrozDatabase> {
    override fun initialize(): CarBrozDatabase
}

@Entity(tableName = "database_metadata")
data class DatabaseMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface DatabaseMetadataDao {
    @Query("SELECT * FROM database_metadata WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): DatabaseMetadataEntity?

    @Upsert
    suspend fun upsert(entity: DatabaseMetadataEntity)

    @Query("DELETE FROM database_metadata WHERE `key` = :key")
    suspend fun remove(key: String)
}

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val operationId: String,
    val method: String,
    val endpoint: String,
    val payloadJson: String?,
    val headersJson: String,
    val authentication: String,
    val idempotencyKey: String,
    val timeoutMillis: Long,
    val maxAttempts: Int,
    val initialRetryDelayMillis: Long,
    val maxRetryDelayMillis: Long,
    val backoffMultiplier: Double,
    val createdAtEpochMilliseconds: Long,
    val attemptCount: Int,
    val nextAttemptAtEpochMilliseconds: Long,
)

@Dao
interface SyncOutboxDao {
    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM sync_outbox WHERE operationId = :operationId)")
    suspend fun contains(operationId: String): Boolean

    @Query(
        "SELECT * FROM sync_outbox " +
            "WHERE nextAttemptAtEpochMilliseconds <= :nowEpochMilliseconds " +
            "ORDER BY createdAtEpochMilliseconds ASC, operationId ASC LIMIT :limit",
    )
    suspend fun ready(nowEpochMilliseconds: Long, limit: Int): List<SyncOutboxEntity>

    @Upsert
    suspend fun upsert(entity: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE operationId = :operationId")
    suspend fun remove(operationId: String)
}
