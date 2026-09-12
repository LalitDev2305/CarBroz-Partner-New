package com.carbroz.feature.dynamic

import com.carbroz.foundation.navigation.NavigationDestination
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiRequestMethod
import kotlinx.serialization.Serializable

/** One complete backend-defined dynamic screen stack entry. */
@Serializable
data class DynamicDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: SduiRequestMethod,
    val authentication: SduiAuthentication,
) : NavigationDestination {
    init {
        require(screenId.isNotBlank()) { "Dynamic screenId must not be blank." }
        require(templateId.isNotBlank()) { "Dynamic templateId must not be blank." }
        require(templateType.isNotBlank()) { "Dynamic templateType must not be blank." }
        require(endpoint.startsWith('/') && !endpoint.startsWith("//") && "://" !in endpoint) {
            "Dynamic endpoint must be a safe relative endpoint."
        }
        require(method == SduiRequestMethod.GET) { "Dynamic destinations must be fetched with GET." }
    }

    override val navigationId: String = "$PREFIX$screenId:$templateId:$endpoint"

    fun toSduiDestination(): SduiDestination = SduiDestination(
        screenId = screenId,
        templateId = templateId,
        templateType = templateType,
        endpoint = endpoint,
        method = method,
        authentication = authentication,
    )

    companion object {
        const val PREFIX: String = "dynamic:"

        fun from(value: SduiDestination): DynamicDestination = DynamicDestination(
            screenId = value.screenId,
            templateId = value.templateId,
            templateType = value.templateType,
            endpoint = value.endpoint,
            method = value.method,
            authentication = value.authentication,
        )
    }
}
