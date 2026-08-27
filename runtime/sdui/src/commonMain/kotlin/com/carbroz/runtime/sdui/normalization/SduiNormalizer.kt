package com.carbroz.runtime.sdui.normalization

import com.carbroz.runtime.sdui.extension.DefinitionKey
import com.carbroz.runtime.sdui.extension.PropertyDecodeResult
import com.carbroz.runtime.sdui.model.BackgroundCommand
import com.carbroz.runtime.sdui.model.BackgroundNetworkRequirement
import com.carbroz.runtime.sdui.model.BackgroundOperation
import com.carbroz.runtime.sdui.model.BackgroundWorkKind
import com.carbroz.runtime.sdui.model.CapabilityCommand
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.Component
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.ConditionalCommand
import com.carbroz.runtime.sdui.model.Element
import com.carbroz.runtime.sdui.model.FormCommand
import com.carbroz.runtime.sdui.model.FormOperation
import com.carbroz.runtime.sdui.model.Group
import com.carbroz.runtime.sdui.model.LocalStateCommand
import com.carbroz.runtime.sdui.model.NavigationOperation
import com.carbroz.runtime.sdui.model.NodeId
import com.carbroz.runtime.sdui.model.NodeKind
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.NodeProperties
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.PresentationCommand
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestCommand
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import com.carbroz.runtime.sdui.model.SduiNavigationCommand
import com.carbroz.runtime.sdui.model.Section
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.model.SequenceCommand
import com.carbroz.runtime.sdui.model.Template
import com.carbroz.runtime.sdui.protocol.BackgroundCommandDto
import com.carbroz.runtime.sdui.protocol.CapabilityCommandDto
import com.carbroz.runtime.sdui.protocol.CommandDto
import com.carbroz.runtime.sdui.protocol.ComponentDto
import com.carbroz.runtime.sdui.protocol.ConditionalCommandDto
import com.carbroz.runtime.sdui.protocol.ElementDto
import com.carbroz.runtime.sdui.protocol.FormCommandDto
import com.carbroz.runtime.sdui.protocol.GroupDto
import com.carbroz.runtime.sdui.protocol.LocalStateCommandDto
import com.carbroz.runtime.sdui.protocol.NavigationCommandDto
import com.carbroz.runtime.sdui.protocol.PresentationCommandDto
import com.carbroz.runtime.sdui.protocol.RequestCommandDto
import com.carbroz.runtime.sdui.protocol.ScreenDto
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SectionDto
import com.carbroz.runtime.sdui.protocol.SequenceCommandDto
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

class SduiNormalizer(private val registry: SduiRegistry) {
    fun normalize(envelope: SduiEnvelopeDto): SduiNormalizationResult = try {
        SduiNormalizationResult.Success(normalizeScreen(envelope.screen))
    } catch (failure: NormalizationFailure) {
        SduiNormalizationResult.Failure(failure.error)
    }

    private fun normalizeScreen(dto: ScreenDto): Screen {
        val screenId = NodeId(dto.id)
        return Screen(screenId, dto.version, normalizeTemplate(dto.template, NodePath.root(screenId)))
    }

    private fun normalizeTemplate(dto: TemplateDto, parentPath: NodePath): Template {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        return Template(
            id,
            path,
            NodeType(dto.type),
            decodeProperties(NodeKind.TEMPLATE, dto.type, dto.properties, path),
            dto.components.map { normalizeComponent(it, path) },
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
            id,
            path,
            NodeType(dto.type),
            decodeProperties(NodeKind.COMPONENT, dto.type, dto.properties, path),
            content,
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
            id,
            path,
            NodeType(dto.type),
            decodeProperties(NodeKind.SECTION, dto.type, dto.properties, path),
            content,
        )
    }

    private fun normalizeGroup(dto: GroupDto, parentPath: NodePath): Group {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        if (dto.elements.isEmpty()) fail(SduiNormalizationError.InvalidStructure(path.toString()))
        return Group(
            id,
            path,
            NodeType(dto.type),
            decodeProperties(NodeKind.GROUP, dto.type, dto.properties, path),
            dto.elements.map { normalizeElement(it, path) },
        )
    }

    private fun normalizeElement(dto: ElementDto, parentPath: NodePath): Element {
        val id = NodeId(dto.id)
        val path = parentPath.child(id)
        return Element(
            id,
            path,
            NodeType(dto.type),
            decodeProperties(NodeKind.ELEMENT, dto.type, dto.properties, path),
            dto.command?.let { normalizeCommand(it, "$path/command") },
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
            is PropertyDecodeResult.Failure -> fail(
                SduiNormalizationError.InvalidProperties(path.toString(), result.reason),
            )
        }
    }

    private fun normalizeCommand(dto: CommandDto, path: String): Command = when (dto) {
        is RequestCommandDto -> normalizeRequest(dto, path)
        is CapabilityCommandDto -> {
            if (dto.capability.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/capability"))
            if (dto.operation.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/operation"))
            CapabilityCommand(dto.capability, dto.operation, dto.arguments.toMap())
        }
        is NavigationCommandDto -> SduiNavigationCommand(
            enumValue(dto.operation, "$path/operation"),
            dto.targetNavigationId,
        )
        is PresentationCommandDto -> {
            if (dto.id.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/id"))
            PresentationCommand(
                enumValue(dto.operation, "$path/operation"),
                enumValue(dto.presentationKind, "$path/presentationKind"),
                dto.id,
                dto.properties.toMap(),
            )
        }
        is LocalStateCommandDto -> LocalStateCommand(dto.values.toMap())
        is FormCommandDto -> FormCommand(enumValue<FormOperation>(dto.operation, "$path/operation"))
        is BackgroundCommandDto -> {
            if (dto.id.isBlank()) fail(SduiNormalizationError.InvalidCommand("$path/id"))
            BackgroundCommand(
                operation = enumValue<BackgroundOperation>(dto.operation, "$path/operation"),
                id = dto.id,
                workKind = enumValue<BackgroundWorkKind>(dto.workKind, "$path/workKind"),
                earliestStartDelayMillis = dto.earliestStartDelayMillis,
                networkRequirement = enumValue<BackgroundNetworkRequirement>(dto.networkRequirement, "$path/networkRequirement"),
                requiresCharging = dto.requiresCharging,
                title = dto.title,
                description = dto.description,
                input = dto.input.toMap(),
            )
        }
        is SequenceCommandDto -> SequenceCommand(
            dto.commands.mapIndexed { index, child -> normalizeCommand(child, "$path/commands[$index]") },
        )
        is ConditionalCommandDto -> ConditionalCommand(
            condition = dto.condition,
            whenTrue = normalizeCommand(dto.whenTrue, "$path/whenTrue"),
            whenFalse = dto.whenFalse?.let { normalizeCommand(it, "$path/whenFalse") },
        )
    }

    private fun normalizeRequest(dto: RequestCommandDto, path: String): RequestCommand {
        val method = enumValue<RequestMethod>(dto.method, "$path/method")
        val authentication = enumValue<RequestAuthentication>(dto.authentication, "$path/authentication")
        val responseMode = enumValue<RequestResponseMode>(dto.responseMode, "$path/responseMode")
        val transition = enumValue<ScreenTransition>(dto.transition, "$path/transition")
        val destination = if (responseMode == RequestResponseMode.SCREEN) {
            val screenId = dto.screenId?.takeIf { it.isNotBlank() }
                ?: fail(SduiNormalizationError.InvalidCommand("$path/screenId"))
            val templateId = dto.templateId?.takeIf { it.isNotBlank() }
                ?: fail(SduiNormalizationError.InvalidCommand("$path/templateId"))
            val templateType = dto.templateType?.takeIf { it.isNotBlank() }
                ?: fail(SduiNormalizationError.InvalidCommand("$path/templateType"))
            ScreenDestination(screenId, templateId, NodeType(templateType))
        } else null
        return RequestCommand(
            method,
            dto.endpoint,
            destination,
            dto.payload.toMap(),
            authentication,
            responseMode,
            transition,
            dto.backStackKey,
            dto.validateForm,
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String, path: String): T =
        enumValues<T>().firstOrNull { it.name == value.uppercase() }
            ?: fail(SduiNormalizationError.InvalidCommand(path))

    private fun fail(error: SduiNormalizationError): Nothing = throw NormalizationFailure(error)
    private class NormalizationFailure(val error: SduiNormalizationError) : RuntimeException()
}
