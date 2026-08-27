package com.carbroz.partner.composition

import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicInstructionDecodeResult
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationRestorer
import com.carbroz.foundation.navigation.NavigationRestorationPolicy
import com.carbroz.foundation.navigation.NavigationRestorationResult
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.foundation.navigation.RestoredDestination
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.Koin
import org.koin.mp.KoinPlatform

object DynamicNavigationProcessStateBridge {
    private val persistence = DynamicNavigationPersistence()
    private val codec = DynamicNavigationProcessStateCodec()

    fun save(): String? {
        val persisted = persistence.persist(navigationStore().state.value)
        return persisted.takeIf { it.isNotEmpty() }?.let(codec::encode)
    }

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

internal class DynamicNavigationPersistence(
    private val instructionCodec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
) : NavigationDestinationRestorer {
    fun persist(destination: NavigationDestination): RestoredDestination? = when (destination) {
        SplashDestination -> RestoredDestination(SplashDestination.navigationId)
        is DynamicDestination -> {
            if (destination.instruction.restorePolicy == DynamicRestorePolicy.CACHE_ONLY) null
            else RestoredDestination(
                navigationId = destination.navigationId,
                payload = instructionCodec.encode(destination.instruction),
            )
        }
        else -> null
    }

    fun persist(state: NavigationState): List<RestoredDestination> {
        val persisted = state.backStack.map { persist(it) }
        return if (persisted.any { it == null }) emptyList() else persisted.filterNotNull()
    }

    fun restoreStack(persisted: List<RestoredDestination>): NavigationRestorationResult =
        NavigationRestorationPolicy.restore(
            persisted = persisted,
            fallbackRoot = SplashDestination,
            restorer = this,
        )

    override fun restore(destination: RestoredDestination): NavigationDestination? {
        if (destination.navigationId == SplashDestination.navigationId) {
            return SplashDestination.takeIf { destination.payload == null }
        }
        if (!destination.navigationId.startsWith(DynamicDestination.PREFIX)) return null
        val payload = destination.payload ?: return null
        val decoded = instructionCodec.decode(payload) as? DynamicInstructionDecodeResult.Success ?: return null
        val restored = DynamicDestination(decoded.instruction)
        return restored.takeIf { it.navigationId == destination.navigationId }
    }
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
