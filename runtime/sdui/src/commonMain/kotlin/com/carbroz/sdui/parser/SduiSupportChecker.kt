package com.carbroz.sdui.parser

import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.registry.SduiNodeRegistry

sealed interface SduiSupportResult {
    data object Supported : SduiSupportResult
    data class Unsupported(val reason: String) : SduiSupportResult
}

class SduiSupportChecker(
    private val registry: SduiNodeRegistry,
    private val versionPolicy: SduiVersionPolicy = SduiVersionPolicy(),
) {
    fun check(screen: SduiScreen): SduiSupportResult {
        if (!versionPolicy.supports(screen.schemaVersion)) {
            return SduiSupportResult.Unsupported("unsupported_schema:${screen.schemaVersion}")
        }
        if (!registry.supportsTemplate(screen.template.type)) {
            return SduiSupportResult.Unsupported("unsupported_template:${screen.template.type}")
        }
        screen.template.components.forEach { component ->
            if (!registry.supportsComponent(component.type)) {
                return SduiSupportResult.Unsupported("unsupported_component:${component.type}")
            }
            component.elements.orEmpty().forEach { element ->
                supportElement(element.type, element.actions.values)?.let { return it }
            }
            component.sections.orEmpty().forEach { section ->
                if (!registry.supportsSection(section.type)) {
                    return SduiSupportResult.Unsupported("unsupported_section:${section.type}")
                }
                section.elements.orEmpty().forEach { element ->
                    supportElement(element.type, element.actions.values)?.let { return it }
                }
                section.groups.orEmpty().forEach { group ->
                    if (!registry.supportsGroup(group.type)) {
                        return SduiSupportResult.Unsupported("unsupported_group:${group.type}")
                    }
                    group.elements.forEach { element ->
                        supportElement(element.type, element.actions.values)?.let { return it }
                    }
                }
            }
        }
        return SduiSupportResult.Supported
    }

    private fun supportElement(type: String, actions: Collection<SduiAction>): SduiSupportResult.Unsupported? {
        if (!registry.supportsElement(type)) {
            return SduiSupportResult.Unsupported("unsupported_element:$type")
        }
        actions.forEach { action -> supportAction(action)?.let { return it } }
        return null
    }

    private fun supportAction(action: SduiAction): SduiSupportResult.Unsupported? = when (action) {
        is SduiAction.Request -> unsafeEndpoint(action.payload.endpoint)
        is SduiAction.Navigate -> unsafeEndpoint(action.payload.endpoint)
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
