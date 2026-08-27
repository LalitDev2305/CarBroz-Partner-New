package com.carbroz.runtime.sdui.compatibility

import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.protocol.CommandDto
import com.carbroz.runtime.sdui.protocol.ConditionalCommandDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SequenceCommandDto
import com.carbroz.runtime.sdui.registry.SduiRegistry

data class SduiClientCompatibility(
    val clientVersion: Int,
    val supportedProtocolVersions: IntRange,
    val supportedSchemaVersions: IntRange,
    val supportedCapabilities: Set<String> = emptySet(),
)

sealed interface SduiCompatibilityResult {
    data object Compatible : SduiCompatibilityResult
    data class Incompatible(val issues: List<SduiCompatibilityIssue>) : SduiCompatibilityResult
}

sealed interface SduiCompatibilityIssue {
    data class ClientTooOld(val minimumClientVersion: Int, val actualClientVersion: Int) : SduiCompatibilityIssue
    data class UnsupportedProtocolVersion(val version: Int) : SduiCompatibilityIssue
    data class UnsupportedSchemaVersion(val version: Int) : SduiCompatibilityIssue
    data class UnsupportedRequiredDefinition(val type: String) : SduiCompatibilityIssue
    data class UnsupportedRequiredCapability(val capability: String) : SduiCompatibilityIssue
    data class UnsupportedRequestDestinationTemplate(val type: String) : SduiCompatibilityIssue
}

/** Compatibility runs only after schema validation; no-screen requests have no destination template to check. */
class SduiCompatibilityPolicy(
    private val client: SduiClientCompatibility,
    private val registry: SduiRegistry,
) {
    fun evaluate(envelope: SduiEnvelopeDto): SduiCompatibilityResult {
        val issues = mutableListOf<SduiCompatibilityIssue>()

        if (client.clientVersion < envelope.minimumClientVersion) {
            issues += SduiCompatibilityIssue.ClientTooOld(envelope.minimumClientVersion, client.clientVersion)
        }
        if (envelope.protocolVersion !in client.supportedProtocolVersions) {
            issues += SduiCompatibilityIssue.UnsupportedProtocolVersion(envelope.protocolVersion)
        }
        if (envelope.schemaVersion !in client.supportedSchemaVersions) {
            issues += SduiCompatibilityIssue.UnsupportedSchemaVersion(envelope.schemaVersion)
        }

        envelope.requiredDefinitions
            .filterNot(::supportsAnyDefinitionType)
            .forEach { issues += SduiCompatibilityIssue.UnsupportedRequiredDefinition(it) }

        envelope.requiredCapabilities
            .filterNot(client.supportedCapabilities::contains)
            .forEach { issues += SduiCompatibilityIssue.UnsupportedRequiredCapability(it) }

        requestCommands(envelope)
            .filter { it.responseMode.equals("SCREEN", ignoreCase = true) }
            .mapNotNull(RequestCommandDto::templateType)
            .filterNot { registry.supports(NodeKind.TEMPLATE, NodeType(it)) }
            .forEach { issues += SduiCompatibilityIssue.UnsupportedRequestDestinationTemplate(it) }

        return if (issues.isEmpty()) SduiCompatibilityResult.Compatible
        else SduiCompatibilityResult.Incompatible(issues)
    }

    private fun supportsAnyDefinitionType(type: String): Boolean =
        NodeKind.entries.any { kind -> registry.supports(kind, NodeType(type)) }

    private fun requestCommands(envelope: SduiEnvelopeDto): List<RequestCommandDto> = buildList {
        envelope.screen.template.components.forEach { component ->
            collectFrom(component.elements)
            component.sections.forEach { section ->
                collectFrom(section.elements)
                section.groups.forEach { group -> collectFrom(group.elements) }
            }
        }
    }

    private fun MutableList<RequestCommandDto>.collectFrom(elements: List<ElementDto>) {
        elements.forEach { element -> element.command?.let { collectCommand(it) } }
    }

    private fun MutableList<RequestCommandDto>.collectCommand(command: CommandDto) {
        when (command) {
            is RequestCommandDto -> add(command)
            is SequenceCommandDto -> command.commands.forEach(::collectCommand)
            is ConditionalCommandDto -> {
                collectCommand(command.whenTrue)
                command.whenFalse?.let(::collectCommand)
            }
            else -> Unit
        }
    }
}
