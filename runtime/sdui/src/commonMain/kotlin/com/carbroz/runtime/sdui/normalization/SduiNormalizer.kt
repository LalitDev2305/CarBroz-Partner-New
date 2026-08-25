package com.carbroz.runtime.sdui.normalization

import com.carbroz.runtime.sdui.extension.DefinitionKey
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.CapabilityCommand
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.Group
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.model.Template
import com.carbroz.runtime.sdui.protocol.CapabilityCommandDto
import com.carbroz.runtime.sdui.protocol.CommandDto
import com.carbroz.runtime.sdui.protocol.ComponentDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.GroupDto
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.ScreenDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SectionDto
import com.carbroz.runtime.sdui.protocol.TemplateDto
import com.carbroz.runtime.sdui.registry.SduiRegistry

sealed interface SduiNormalizationResult {
    data class Success(val screen: Screen) : SduiNormalizationResult
    data class Failure(val error: SduiNormalizationError) : SduiNormalizationResult
}

sealed interface SduiNormalizationError {
    data class UnsupportedDefinition(val path: String, val key: DefinitionKey) : SduiNormalizationError
    data class InvalidProperties(val path: String, val reason: String) : SduiNormalizationError
    data class InvalidStructure(val path: String) : SduiNormalizationError
    data class InvalidCommand(val path: String) : SduiNormalizationError
}

/** Converts already schema-validated transport data into immutable trusted runtime IR. */
class SduiNormalizer(
    private val registry: SduiRegistry,
) {
    fun normalize(envelope: SduiEnvelopeDto): SduiNormalizationResult = try {
        SduiNormalizationResult.Success(normalizeScreen(envelope.screen))
    } catch (failure: NormalizationFailure) {
        SduiNormalizationResult.Failure(failure.error)
    }

    private fun normalizeScreen(dto: ScreenDto): Screen {
        val screenId = NodeId(dto.id)
        val root = NodePath.root(screenId)
        return Screen(
            id = screenId,
            version = dto.version,
            template = normalizeTemplate(dto.template, root),
        )
    }

    private fun normalizeTemplate(dto: TemplateDto, parentPath: NodePath): Template {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        return Template(
            id = id,
            path = path,
            type = NodeType(dto.type),
            properties = decodeProperties(NodeKind.TEMPLATE, dto.type, dto.properties, path),
            components = dto.components.map { normalizeComponent(it, path) },
        )
    }

    private fun normalizeComponent(dto: ComponentDto, parentPath: NodePath): Component {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        val content = when {
            dto.sections.isNotEmpty() && dto.elements.isEmpty() ->
                ComponentContent.Sections(dto.sections.map { normalizeSection(it, path) })
            dto.elements.isNotEmpty() && dto.sections.isEmpty() ->
                ComponentContent.Elements(dto.elements.map { normalizeElement(it, path) })
            else -> fail(SduiNormalizationError.InvalidStructure(path.toString()))
        }
        return Component(
            id = id,
            path = path,
            type = NodeType(dto.type),
            properties = decodeProperties(NodeKind.COMPONENT, dto.type, dto.properties, path),
            content = content,
        )
    }

    private fun normalizeSection(dto: SectionDto, parentPath: NodePath): Section {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        val content = when {
            dto.groups.isNotEmpty() && dto.elements.isEmpty() ->
                SectionContent.Groups(dto.groups.map { normalizeGroup(it, path) })
            dto.elements.isNotEmpty() && dto.groups.isEmpty() ->
                SectionContent.Elements(dto.elements.map { normalizeElement(it, path) })
            else -> fail(SduiNormalizationError.InvalidStructure(path.toString()))
        }
        return Section(
            id = id,
            path = path,
            type = NodeType(dto.type),
            properties = decodeProperties(NodeKind.SECTION, dto.type, dto.properties, path),
            content = content,
        )
    }

    private fun normalizeGroup(dto: GroupDto, parentPath: NodePath): Group {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        if (dto.elements.isEmpty()) fail(SduiNormalizationError.InvalidStructure(path.toString()))
        return Group(
            id = id,
            path = path,
            type = NodeType(dto.type),
            properties = decodeProperties(NodeKind.GROUP, dto.type, dto.properties, path),
            elements = dto.elements.map { normalizeElement(it, path) },
        )
    }

    private fun normalizeElement(dto: ElementDto, parentPath: NodePath): Element {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        return Element(
            id = id,
            path = path,
            type = NodeType(dto.type),
            properties = decodeProperties(NodeKind.ELEMENT, dto.type, dto.properties, path),
            command = dto.command?.let { normalizeCommand(it, "$path/command") },
        )
    }

    private fun decodeProperties(
        kind: NodeKind,
        typeValue: String,
        raw: kotlinx.serialization.json.JsonObject,
        path: NodePath,
    ): NodeProperties {
        val type = NodeType(typeValue)
        val key = DefinitionKey(kind, type)
        val definition = registry.find(kind, type)
            ?: fail(SduiNormalizationError.UnsupportedDefinition(path.toString(), key))
        return when (val result = definition.decodeProperties(raw)) {
            is PropertyDecodeResult.Success -> result.properties
            is PropertyDecodeResult.Failure ->
                fail(SduiNormalizationError.InvalidProperties(path.toString(), result.reason))
        }
    }

    private fun normalizeCommand(dto: CommandDto, path: String): Command = when (dto) {
        is RequestCommandDto -> {
            val method = RequestMethod.entries.firstOrNull { it.name == dto.method.uppercase() }
                ?: fail(SduiNormalizationError.InvalidCommand("$path/method"))
            RequestCommand(
                method = method,
                endpoint = dto.endpoint,
                destination = ScreenDestination(
                    screenId = dto.screenId,
                    templateId = dto.templateId,
                    templateType = NodeType(dto.templateType),
                ),
                payload = dto.payload.toMap(),
            )
        }
        is CapabilityCommandDto -> {
            if (dto.capability.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/capability"))
            if (dto.operation.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/operation"))
            CapabilityCommand(
                capability = dto.capability,
                operation = dto.operation,
                arguments = dto.arguments.toMap(),
            )
        }
    }

    private fun fail(error: SduiNormalizationError): Nothing = throw NormalizationFailure(error)

    private class NormalizationFailure(val error: SduiNormalizationError) : RuntimeException()
}
