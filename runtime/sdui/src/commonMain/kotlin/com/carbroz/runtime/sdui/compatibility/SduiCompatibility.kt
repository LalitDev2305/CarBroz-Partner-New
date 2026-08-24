package com.carbroz.runtime.sdui.compatibility

import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.registry.SduiRegistry

/** Client-side protocol/schema support declared by the application runtime. */
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
    data class ClientTooOld(
        val minimumClientVersion: Int,
        val actualClientVersion: Int,
    ) : SduiCompatibilityIssue

    data class UnsupportedProtocolVersion(val version: Int) : SduiCompatibilityIssue
    data class UnsupportedSchemaVersion(val version: Int) : SduiCompatibilityIssue
    data class UnsupportedRequiredDefinition(val type: String) : SduiCompatibilityIssue
    data class UnsupportedRequiredCapability(val capability: String) : SduiCompatibilityIssue
    data class UnsupportedRequestDestinationTemplate(val type: String) : SduiCompatibilityIssue
}

/**
 * Evaluates client support before normalization/rendering and before a REQUEST command
 * can later execute. It intentionally does not perform networking or capability execution.
 */
class SduiCompatibilityPolicy(
    private val client: SduiClientCompatibility,
    private val registry: SduiRegistry,
) {
    fun evaluate(envelope: SduiEnvelopeDto): SduiCompatibilityResult {
        val issues = mutableListOf<SduiCompatibilityIssue>()

        if (client.clientVersion < envelope.minimumClientVersion) {
            issues += SduiCompatibilityIssue.ClientTooOld(
                minimumClientVersion = envelope.minimumClientVersion,
                actualClientVersion = client.clientVersion,
            )
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

        requestCommands(envelope).forEach { command ->
            if (!registry.supports(NodeKind.TEMPLATE, NodeType(command.templateType))) {
                issues += SduiCompatibilityIssue.UnsupportedRequestDestinationTemplate(command.templateType)
            }
        }

        return if (issues.isEmpty()) SduiCompatibilityResult.Compatible
        else SduiCompatibilityResult.Incompatible(issues)
    }

    private fun supportsAnyDefinitionType(type: String): Boolean =
        NodeKind.entries.any { kind -> registry.supports(kind, NodeType(type)) }

    private fun requestCommands(envelope: SduiEnvelopeDto): Sequence<RequestCommandDto> = sequence {
        envelope.screen.template.components.forEach { component ->
            component.elements.mapNotNullToCommand(this)
            component.sections.forEach { section ->
                section.elements.mapNotNullToCommand(this)
                section.groups.forEach { group ->
                    group.elements.mapNotNullToCommand(this)
                }
            }
        }
    }

    private fun List<com.carbroz.runtime.sdui.protocol.ElementDto>.mapNotNullToCommand(
        scope: SequenceScope<RequestCommandDto>,
    ) {
        forEach { element ->
            val command = element.command
            if (command is RequestCommandDto) {
                scope.yield(command)
            }
        }
    }
}
