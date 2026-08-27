package com.carbroz.partner.composition

import com.carbroz.feature.splash.SplashDestination
import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationRestorer
import com.carbroz.foundation.navigation.NavigationRestorationPolicy
import com.carbroz.foundation.navigation.NavigationRestorationResult
import com.carbroz.foundation.navigation.NavigationState
import com.carbroz.foundation.navigation.RestoredDestination
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonObject

/** Trusted application-owned instruction for acquiring one backend-driven screen. */
data class DynamicScreenInstruction(
    val destination: ScreenDestination,
    val request: DynamicScreenRequest,
    val transition: ScreenTransition = ScreenTransition.RESET,
    val backStackKey: String = destination.screenId,
    val restorePolicy: DynamicRestorePolicy = DynamicRestorePolicy.CACHE_FIRST,
) {
    init { require(backStackKey.isNotBlank()) { "Dynamic back-stack key must not be blank." } }
}

data class DynamicScreenRequest(
    val method: RequestMethod,
    val endpoint: String,
    val payload: JsonObject = JsonObject(emptyMap()),
    val authentication: RequestAuthentication = RequestAuthentication.SESSION,
) {
    init {
        require(endpoint.startsWith('/')) { "Dynamic screen endpoint must be relative." }
        require(!endpoint.startsWith("//")) { "Dynamic screen endpoint must not be protocol-relative." }
        require("://" !in endpoint) { "Dynamic screen endpoint must not contain an absolute URL." }
    }
}

/** CACHE_ONLY prevents unsafe replay of destinations acquired through non-idempotent requests. */
enum class DynamicRestorePolicy {
    CACHE_ONLY,
    CACHE_FIRST,
    REFRESH,
    NETWORK_ONLY,
}

data class DynamicDestination(
    val instruction: DynamicScreenInstruction,
) : NavigationDestination {
    override val navigationId: String = buildString {
        append(PREFIX)
        append(instruction.backStackKey)
        append(':')
        append(instruction.destination.screenId)
        append(':')
        append(instruction.destination.templateId)
    }

    companion object {
        const val PREFIX: String = "dynamic:"
    }
}

/**
 * Converts dynamic destinations to the foundation restoration contract. CACHE_ONLY entries are not
 * process-restorable because recreating them could replay a non-idempotent request.
 */
class DynamicNavigationPersistence(
    private val codec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
) : NavigationDestinationRestorer {
    fun persist(destination: NavigationDestination): RestoredDestination? = when (destination) {
        SplashDestination -> RestoredDestination(SplashDestination.navigationId)
        is DynamicDestination -> {
            if (destination.instruction.restorePolicy == DynamicRestorePolicy.CACHE_ONLY) null
            else RestoredDestination(
                navigationId = destination.navigationId,
                payload = codec.encode(destination.instruction),
            )
        }
        else -> null
    }

    /**
     * A stack is persisted atomically. If any entry is unsafe or unknown, an empty persisted stack is
     * returned so the foundation restoration policy falls back to Splash/bootstrap rather than
     * restoring a partial or semantically different stack.
     */
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
        val decoded = codec.decode(payload) as? DynamicInstructionDecodeResult.Success ?: return null
        val restored = DynamicDestination(decoded.instruction)
        return restored.takeIf { it.navigationId == destination.navigationId }
    }
}
