package com.carbroz.feature.dynamic

import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.runtime.application.startup.StartupPayload
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.json.JsonObject

/** Trusted feature-owned instruction for acquiring one backend-driven screen. */
data class DynamicScreenInstruction(
    val destination: ScreenDestination,
    val request: DynamicScreenRequest,
    val transition: ScreenTransition = ScreenTransition.RESET,
    val backStackKey: String = destination.screenId,
    val restorePolicy: DynamicRestorePolicy = DynamicRestorePolicy.CACHE_FIRST,
) : StartupPayload {
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

enum class DynamicRestorePolicy {
    CACHE_ONLY,
    CACHE_FIRST,
    REFRESH,
    NETWORK_ONLY,
}

/** Feature-owned semantic destination. Stack mechanics remain owned by foundation:navigation. */
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
