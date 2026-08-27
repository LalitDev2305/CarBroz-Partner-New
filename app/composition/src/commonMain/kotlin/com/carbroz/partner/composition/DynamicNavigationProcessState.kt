package com.carbroz.partner.composition

import com.carbroz.foundation.navigation.NavigationRestorationResult
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.navigation.RestoredDestination
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import org.koin.mp.KoinPlatform

/**
 * Opaque, host-owned process-state representation of the semantic navigation stack.
 *
 * The returned value is intended only for transient platform saved-state containers
 * (for example Android's instance state). It must not be written to durable preferences
 * because a dynamic instruction can contain arbitrary request payload data.
 */
object DynamicNavigationProcessStateBridge {
    private val persistence = DynamicNavigationPersistence()
    private val codec = DynamicNavigationProcessStateCodec()

    fun save(): String? {
        val persisted = persistence.persist(navigationStore().state.value)
        return persisted.takeIf { it.isNotEmpty() }?.let(codec::encode)
    }

    /**
     * Restores the complete stack atomically. Malformed, unsafe, or unknown state falls back
     * to Splash so normal bootstrap can reacquire a trusted backend-driven destination.
     */
    fun restore(encodedState: String?): NavigationRestorationResult? {
        if (encodedState.isNullOrBlank()) return null
        val persisted = codec.decode(encodedState).orEmpty()
        val result = persistence.restoreStack(persisted)
        val state = when (result) {
            is NavigationRestorationResult.Restored -> result.state
            is NavigationRestorationResult.Fallback -> result.state
        }
        navigationStore().restore(state)
        return result
    }

    private fun navigationStore(): NavigationStore = koin().get()

    private fun koin(): Koin = KoinPlatform.getKoinOrNull()
        ?: error("CarBroz dependency injection must be initialized before navigation process state is accessed.")
}

internal class DynamicNavigationProcessStateCodec(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    },
) {
    fun encode(destinations: List<RestoredDestination>): String = json.encodeToString(
        ProcessStateDto(
            destinations = destinations.map {
                RestoredDestinationDto(
                    navigationId = it.navigationId,
                    payload = it.payload,
                )
            },
        ),
    )

    fun decode(encodedState: String): List<RestoredDestination>? = runCatching {
        json.decodeFromString<ProcessStateDto>(encodedState).destinations.map {
            RestoredDestination(
                navigationId = it.navigationId,
                payload = it.payload,
            )
        }
    }.getOrNull()
}

@Serializable
private data class ProcessStateDto(
    val destinations: List<RestoredDestinationDto>,
)

@Serializable
private data class RestoredDestinationDto(
    val navigationId: String,
    val payload: String? = null,
)
