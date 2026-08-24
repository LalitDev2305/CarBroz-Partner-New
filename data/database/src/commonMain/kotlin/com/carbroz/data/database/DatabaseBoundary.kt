package com.carbroz.data.database

sealed interface DatabaseHealth {
    data object Healthy : DatabaseHealth
    data object Unavailable : DatabaseHealth
}

fun interface DatabaseHealthCheck {
    suspend fun check(): DatabaseHealth
}

class RoomDatabaseHealthCheck(
    private val database: CarBrozDatabase,
) : DatabaseHealthCheck {
    override suspend fun check(): DatabaseHealth =
        runCatching {
            database.metadataDao().get(HEALTH_KEY)
        }.fold(
            onSuccess = { DatabaseHealth.Healthy },
            onFailure = { DatabaseHealth.Unavailable },
        )

    private companion object {
        const val HEALTH_KEY = "health"
    }
}
