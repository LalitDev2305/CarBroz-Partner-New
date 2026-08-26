package com.carbroz.partner.composition

import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.serialization.json.JsonObject

/**
 * Trusted application-owned instruction for acquiring one backend-driven screen.
 * Business screen meaning is deliberately absent: the client only knows identity,
 * renderer type and a trusted relative request.
 */
data class DynamicScreenInstruction(
    val destination: ScreenDestination,
    val request: DynamicScreenRequest,
    val transition: DynamicTransition = DynamicTransition.RESET,
    val backStackKey: String = destination.screenId,
) {
    init {
        require(backStackKey.isNotBlank()) { "Dynamic back-stack key must not be blank." }
    }
}

data class DynamicScreenRequest(
    val method: RequestMethod,
    val endpoint: String,
    val payload: JsonObject = JsonObject(emptyMap()),
) {
    init {
        require(endpoint.startsWith('/')) { "Dynamic screen endpoint must be relative." }
        require(!endpoint.startsWith("//")) { "Dynamic screen endpoint must not be protocol-relative." }
    }
}

enum class DynamicTransition {
    PUSH,
    REPLACE,
    RESET,
    STAY,
}

/** The only generic navigation destination needed for backend-driven screens. */
data class DynamicDestination(
    val instruction: DynamicScreenInstruction,
) : NavigationDestination {
    override val navigationId: String = buildString {
        append("dynamic:")
        append(instruction.backStackKey)
        append(':')
        append(instruction.destination.screenId)
        append(':')
        append(instruction.destination.templateId)
    }
}
