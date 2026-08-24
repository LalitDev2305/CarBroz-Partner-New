package com.carbroz.data.database

import androidx.room3.RoomDatabase
import androidx.room3.setDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/** Platform-owned source of a Room database builder for the canonical application database. */
fun interface CarBrozDatabaseBuilderProvider {
    fun builder(): RoomDatabase.Builder<CarBrozDatabase>
}

/** Builds the canonical database with the shared production driver configuration. */
class CarBrozDatabaseFactory(
    private val builderProvider: CarBrozDatabaseBuilderProvider,
) {
    fun create(): CarBrozDatabase = builderProvider.builder()
        .setDriver(BundledSQLiteDriver())
        .build()
}
