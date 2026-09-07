package com.carbroz.partner.composition

import com.carbroz.feature.dynamic.DynamicDestination
import com.carbroz.feature.dynamic.DynamicInstructionDecodeResult
import com.carbroz.feature.dynamic.DynamicRestorePolicy
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.feature.splash.SplashDestination
import com.carbroz.foundation.navigation.NavigationCommand
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

/**
 * Platform-facing process-state bridge.
 *
 * Saved navigation is captured during host restoration but never applied before fresh startup. After
 * bootstrap resolves the current server-authoritative root, a compatible saved stack may be restored.
 */
object NavigationProcessStateBridge {
    private val persistence = ApplicationNavigationPersistence()
    private val codec = NavigationProcessStateCodec()
    private var pendingEncodedState: String? = null

    fun save(): String? {
        val persisted = persistence.persist(navigationStore().state.value)
        return persisted.takeIf { it.isNotEmpty() }?.let(codec::encode)
    }

    /** Captures platform process state. NavigationStore intentionally remains rooted at Splash. */
    fun restore(encodedState: String?) {
        pendingEncodedState = encodedState?.takeIf(String::isNotBlank)
    }

    /** Applies a saved stack only when its root exactly matches the fresh bootstrap root. */
    fun applyAfterBootstrap(freshRoot: DynamicDestination) {
        val encoded = pendingEncodedState
        pendingEncodedState = null

        val restored = encoded
            ?.let(codec::decode)
            ?.let(persistence::restoreStack)
            ?.stateOrNull()

        if (restored == null) {
            navigationStore().dispatch(NavigationCommand.ResetTo(freshRoot))
            return
        }

        val compatible = restored.backStack
            .firstOrNull()
            ?.navigationId == freshRoot.navigationId

        if (compatible) {
            navigationStore().restore(restored)
        } else {
            navigationStore().dispatch(NavigationCommand.ResetTo(freshRoot))
        }
    }

    private fun NavigationRestorationResult.stateOrNull(): NavigationState = when (this) {
        is NavigationRestorationResult.Restored -> state
        is NavigationRestorationResult.Fallback -> state
    }

    private fun navigationStore(): NavigationStore = koin().get()

    private fun koin(): Koin = KoinPlatform.getKoinOrNull()
        ?: error("CarBroz dependency injection must be initialized before navigation process state is accessed.")
}

internal class ApplicationNavigationPersistence(
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

internal class NavigationProcessStateCodec(
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
