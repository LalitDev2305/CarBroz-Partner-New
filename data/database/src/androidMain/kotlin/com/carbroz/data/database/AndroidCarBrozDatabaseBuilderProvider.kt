package com.carbroz.data.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Android Room builder backed by the app-private database directory. */
class AndroidCarBrozDatabaseBuilderProvider(
    context: Context,
) : CarBrozDatabaseBuilderProvider {
    private val applicationContext = context.applicationContext

    override fun builder(): RoomDatabase.Builder<CarBrozDatabase> = Room.databaseBuilder<CarBrozDatabase>(
        context = applicationContext,
        name = applicationContext.getDatabasePath(CarBrozDatabase.DATABASE_NAME).absolutePath,
    )
}
