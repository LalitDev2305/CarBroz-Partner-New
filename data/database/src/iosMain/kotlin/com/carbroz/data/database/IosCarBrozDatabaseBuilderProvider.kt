package com.carbroz.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** iOS Room builder rooted in Application Support. */
class IosCarBrozDatabaseBuilderProvider : CarBrozDatabaseBuilderProvider {
    @OptIn(ExperimentalForeignApi::class)
    override fun builder(): RoomDatabase.Builder<CarBrozDatabase> {
        val fileManager = NSFileManager.defaultManager
        val supportDirectory = fileManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ) ?: error("Application Support directory is unavailable")
        val databaseUrl = supportDirectory.URLByAppendingPathComponent(CarBrozDatabase.DATABASE_NAME)
            ?: error("Database URL could not be resolved")
        return Room.databaseBuilder<CarBrozDatabase>(name = databaseUrl.path ?: error("Database path is unavailable"))
    }
}
