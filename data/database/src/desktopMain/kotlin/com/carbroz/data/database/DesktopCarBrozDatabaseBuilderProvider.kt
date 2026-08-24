package com.carbroz.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

/** Desktop Room builder rooted in an application-owned database directory. */
class DesktopCarBrozDatabaseBuilderProvider(
    private val databaseDirectory: File,
) : CarBrozDatabaseBuilderProvider {
    override fun builder(): RoomDatabase.Builder<CarBrozDatabase> {
        databaseDirectory.mkdirs()
        return Room.databaseBuilder<CarBrozDatabase>(
            name = File(databaseDirectory, CarBrozDatabase.DATABASE_NAME).absolutePath,
        )
    }
}
