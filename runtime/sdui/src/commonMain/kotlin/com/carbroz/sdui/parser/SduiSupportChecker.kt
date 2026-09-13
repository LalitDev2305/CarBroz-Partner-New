package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAccessory
import com.carbroz.sdui.model.SduiElement
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiTargetApp
import com.carbroz.sdui.model.walk
import com.carbroz.sdui.registry.SduiNodeRegistry
import com.carbroz.sdui.render.accessory.AccessoryRenderer
import com.carbroz.sdui.render.layout.linearChildLayoutSupportError
import kotlinx.serialization.json.JsonArray

sealed interface SduiSupportResult {
    data object Supported : SduiSupportResult
    data class Unsupported(val reason: String) : SduiSupportResult
}

class SduiSupportChecker(
    private val registry: SduiNodeRegistry,
    private val versionPolicy: SduiVersionPolicy = SduiVersionPolicy(),
    private val decoder: SduiDecoder = SduiDecoder(),
) {
    fun check(screen: SduiScreen): SduiSupportResult {
        if (!versionPolicy.supports(screen.schemaVersion)) {
            return SduiSupportResult.Unsupported("unsupported_schema:${screen.schemaVersion}")
        }
        if (screen.targetApp != SduiTargetApp.PARTNER && screen.targetApp != SduiTargetApp.GLOBAL) {
            return SduiSupportResult.Unsupported("unsupported_target_app:${screen.targetApp}")
        }
        if (screen.theme != null) {
            return SduiSupportResult.Unsupported("unsupported_theme")
        }
        if (!registry.supportsTemplate(screen.template.type)) {
            return SduiSupportResult.Unsupported("unsupported_template:${screen.template.type}")
        }
        linearChildLayoutSupportError(screen.template.properties)?.let {
            return SduiSupportResult.Unsupported(it)
        }

        var failure: SduiSupportResult.Unsupported? = null
        screen.template.walk(
            onComponent = { component ->
                if (failure == null && !registry.supportsComponent(component.type)) {
                    failure = SduiSupportResult.Unsupported("unsupported_component:${component.type}")
                }
                if (failure == null) {
                    linearChildLayoutSupportError(component.properties)?.let {
                        failure = SduiSupportResult.Unsupported(it)
                    }
                }
            },
            onSection = { section ->
                if (failure == null && !registry.supportsSection(section.type)) {
                    failure = SduiSupportResult.Unsupported("unsupported_section:${section.type}")
                }
                if (failure == null) {
                    linearChildLayoutSupportError(section.properties)?.let {
                        failure = SduiSupportResult.Unsupported(it)
                    }
                }
            },
            onGroup = { group ->
                if (failure == null && !registry.supportsGroup(group.type)) {
                    failure = SduiSupportResult.Unsupported("unsupported_group:${group.type}")
                }
                if (failure == null) {
                    linearChildLayoutSupportError(group.properties)?.let {
                        failure = SduiSupportResult.Unsupported(it)
                    }
                }
            },
            onElement = { element ->
                if (failure == null) failure = supportElement(element)
            },
        )
        return failure ?: SduiSupportResult.Supported
    }

    private fun supportElement(element: SduiElement): SduiSupportResult.Unsupported? {
        if (!registry.supportsElement(element.type)) {
            return SduiSupportResult.Unsupported("unsupported_element:${element.type}")
        }
        if (element.type == "input" && "weight" in element.properties) {
            return SduiSupportResult.Unsupported("unsupported_input_weight")
        }
        element.actions.values.forEach { action -> supportAction(action)?.let { return it } }
        val embedded = element.readEmbeddedActions(decoder)
        if (embedded.invalid) return SduiSupportResult.Unsupported("unsupported_action")
        embedded.actions.forEach { action -> supportAction(action)?.let { return it } }
        supportPropertyAccessories(element)?.let { return it }
        return null
    }

    private fun supportPropertyAccessories(element: SduiElement): SduiSupportResult.Unsupported? {
        listOf("leading", "trailing").forEach { key ->
            val raw = element.properties[key] ?: return@forEach
            val encodedAccessories = if (raw is JsonArray) raw else JsonArray(listOf(raw))
            encodedAccessories.forEach { encoded ->
                val accessory = decoder.decodeAccessory(encoded)
                    ?: return SduiSupportResult.Unsupported("unsupported_accessory")
                supportAccessory(accessory)?.let { return it }
            }
        }
        return null
    }

    private fun supportAccessory(accessory: SduiAccessory): SduiSupportResult.Unsupported? =
        if (AccessoryRenderer.supports(accessory.type)) null
        else SduiSupportResult.Unsupported("unsupported_accessory:${accessory.type}")

    private fun supportAction(action: SduiAction): SduiSupportResult.Unsupported? = when (action) {
        is SduiAction.Request -> unsafeEndpoint(action.payload.endpoint)
        is SduiAction.Navigate -> {
            if (action.payload.method != SduiRequestMethod.GET) {
                SduiSupportResult.Unsupported("unsupported_destination_method:${action.payload.method}")
            } else {
                unsafeEndpoint(action.payload.endpoint)
            }
        }
        is SduiAction.Sequence -> {
            action.payload.actions.forEach { child -> supportAction(child)?.let { return it } }
            null
        }
        else -> null
    }

    private fun unsafeEndpoint(endpoint: String): SduiSupportResult.Unsupported? =
        if (endpoint.startsWith('/') && !endpoint.startsWith("//") && "://" !in endpoint) null
        else SduiSupportResult.Unsupported("unsafe_endpoint")
}
