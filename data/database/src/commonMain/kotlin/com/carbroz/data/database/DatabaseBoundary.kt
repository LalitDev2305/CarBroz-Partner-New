package com.carbroz.data.database

import com.carbroz.foundation.observability.DiagnosticAttribute
import com.carbroz.foundation.observability.LogEvent
import com.carbroz.foundation.observability.LogLevel
import com.carbroz.foundation.observability.NoOpObservability
import com.carbroz.foundation.observability.Observability
import com.carbroz.foundation.observability.PerformanceMetric
import com.carbroz.foundation.time.Clock
import com.carbroz.foundation.time.SystemClock
import kotlinx.coroutines.CancellationException

sealed interface DatabaseHealth {
    data object Healthy : DatabaseHealth
    data object Unavailable : DatabaseHealth
}

fun interface DatabaseHealthCheck {
    suspend fun check(): DatabaseHealth
}

class RoomDatabaseHealthCheck(
    private val database: CarBrozDatabase,
    private val observability: Observability = NoOpObservability,
    private val clock: Clock = SystemClock,
) : DatabaseHealthCheck {
    override suspend fun check(): DatabaseHealth {
        val startedAt = clock.nowEpochMilliseconds()
        val result = try {
            database.metadataDao().get(HEALTH_KEY)
            DatabaseHealth.Healthy
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            DatabaseHealth.Unavailable
        }

        val outcome = if (result == DatabaseHealth.Healthy) "healthy" else "unavailable"
        observability.performance(
            PerformanceMetric(
                name = "database.health_check",
                durationMillis = (clock.nowEpochMilliseconds() - startedAt).coerceAtLeast(0L),
                attributes = mapOf("outcome" to DiagnosticAttribute(outcome)),
            ),
        )
        if (result == DatabaseHealth.Unavailable) {
            observability.log(
                LogEvent(
                    level = LogLevel.WARN,
                    category = "database",
                    message = "database_health_unavailable",
                ),
            )
        }
        return result
    }

    private companion object {
        const val HEALTH_KEY = "health"
    }
}
