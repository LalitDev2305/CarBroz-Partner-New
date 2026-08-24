package com.carbroz.data.database

import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Platform-owned source of a Room database builder for the canonical application database. */
fun interface CarBrozDatabaseBuilderProvider {
    fun builder(): RoomDatabase.Builder<CarBrozDatabase>
}

/** Composition-facing provider that hides Room builder mechanics from higher layers. */
fun interface CarBrozDatabaseProvider {
    fun get(): CarBrozDatabase
}

/** Builds the canonical database with the shared production driver configuration. */
class CarBrozDatabaseFactory(
    private val builderProvider: CarBrozDatabaseBuilderProvider,
) : CarBrozDatabaseProvider {
    override fun get(): CarBrozDatabase = builderProvider.builder()
        .setDriver(BundledSQLiteDriver())
        .build()
}
