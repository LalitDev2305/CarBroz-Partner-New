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

/**
 * Canonical application database.
 *
 * Business entities are intentionally absent until their owning repository contracts exist.
 * The metadata row is infrastructure-owned and gives the database a real schema anchor for
 * versioning, migrations, health checks, and deterministic bootstrap verification.
 */
@Database(
    entities = [DatabaseMetadataEntity::class],
    version = CarBrozDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@ConstructedBy(CarBrozDatabaseConstructor::class)
abstract class CarBrozDatabase : RoomDatabase() {
    abstract fun metadataDao(): DatabaseMetadataDao

    companion object {
        const val SCHEMA_VERSION: Int = 1
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
