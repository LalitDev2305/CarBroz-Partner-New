package com.carbroz.partner.composition

import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.foundation.navigation.NavigationDestinationRestorer
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
 * process-restorable because recreating them could replay a non-idempotent request. They therefore
 * deliberately fall back to the static root/bootstrap path after process death.
 */
class DynamicNavigationPersistence(
    private val codec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
) : NavigationDestinationRestorer {
    fun persist(destination: NavigationDestination): RestoredDestination? {
        val dynamic = destination as? DynamicDestination ?: return null
        if (dynamic.instruction.restorePolicy == DynamicRestorePolicy.CACHE_ONLY) return null
        return RestoredDestination(
            navigationId = dynamic.navigationId,
            payload = codec.encode(dynamic.instruction),
        )
    }

    override fun restore(destination: RestoredDestination): NavigationDestination? {
        if (!destination.navigationId.startsWith(DynamicDestination.PREFIX)) return null
        val payload = destination.payload ?: return null
        val decoded = codec.decode(payload) as? DynamicInstructionDecodeResult.Success ?: return null
        val restored = DynamicDestination(decoded.instruction)
        return restored.takeIf { it.navigationId == destination.navigationId }
    }
}
