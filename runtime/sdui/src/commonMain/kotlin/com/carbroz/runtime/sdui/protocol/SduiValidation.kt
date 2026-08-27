package com.carbroz.runtime.sdui.protocol

data class SduiProtocolLimits(
    val maxPayloadCharacters: Int = 1_000_000,
    val maxIdentifierLength: Int = 128,
    val maxTypeLength: Int = 128,
    val maxEndpointLength: Int = 512,
    val maxCommandPayloadFields: Int = 128,
    val maxRequiredIdentifiers: Int = 128,
    val maxComponents: Int = 256,
    val maxSectionsPerComponent: Int = 128,
    val maxGroupsPerSection: Int = 256,
    val maxElementsPerContainer: Int = 256,
    val maxTotalNodes: Int = 4_096,
)

sealed interface SduiValidationResult {
    data object Valid : SduiValidationResult
    data class Invalid(val violations: List<SduiViolation>) : SduiValidationResult
}

data class SduiViolation(val path: String, val code: SduiViolationCode)

enum class SduiViolationCode {
    MissingIdentifier,
    IdentifierTooLong,
    MissingType,
    TypeTooLong,
    DuplicateSiblingIdentifier,
    EmptyBranch,
    ConflictingBranchContent,
    MissingTerminalElements,
    CollectionLimitExceeded,
    TotalNodeLimitExceeded,
    UnsupportedRequestMethod,
    UnsupportedCommandValue,
    MissingEndpoint,
    EndpointTooLong,
    EndpointMustBeRelative,
    InvalidCommandPayload,
}

/** Strict structural and generic-command validator for untrusted transport data. */
class SduiSchemaValidator(private val limits: SduiProtocolLimits = SduiProtocolLimits()) {
    fun validate(envelope: SduiEnvelopeDto): SduiValidationResult {
        val context = ValidationContext(limits)
        context.identifier(envelope.screen.id, "screen.id")
        context.identifier(envelope.screen.template.id, "screen.template.id")
        context.type(envelope.screen.template.type, "screen.template.type")
        context.bounded(envelope.requiredDefinitions.size, limits.maxRequiredIdentifiers, "requiredDefinitions")
        context.bounded(envelope.requiredCapabilities.size, limits.maxRequiredIdentifiers, "requiredCapabilities")
        envelope.requiredDefinitions.forEachIndexed { index, value -> context.type(value, "requiredDefinitions[$index]") }
        envelope.requiredCapabilities.forEachIndexed { index, value -> context.type(value, "requiredCapabilities[$index]") }
        context.template(envelope.screen.template, "screen.template")
        return if (context.violations.isEmpty()) SduiValidationResult.Valid else SduiValidationResult.Invalid(context.violations.toList())
    }

    private class ValidationContext(private val limits: SduiProtocolLimits) {
        val violations = mutableListOf<SduiViolation>()
        private var nodes = 0

        fun identifier(value: String, path: String) {
            if (value.isBlank()) violation(path, SduiViolationCode.MissingIdentifier)
            else if (value.length > limits.maxIdentifierLength) violation(path, SduiViolationCode.IdentifierTooLong)
        }

        private fun optionalIdentifier(value: String?, path: String) {
            value?.let { identifier(it, path) }
        }

        fun type(value: String, path: String) {
            if (value.isBlank()) violation(path, SduiViolationCode.MissingType)
            else if (value.length > limits.maxTypeLength) violation(path, SduiViolationCode.TypeTooLong)
        }

        private fun optionalType(value: String?, path: String) {
            value?.let { type(it, path) }
        }

        fun bounded(size: Int, max: Int, path: String) {
            if (size > max) violation(path, SduiViolationCode.CollectionLimitExceeded)
        }

        private fun count(path: String) {
            nodes++
            if (nodes == limits.maxTotalNodes + 1) violation(path, SduiViolationCode.TotalNodeLimitExceeded)
        }

        private fun <T> validateSiblingIds(values: List<T>, path: String, id: (T) -> String) {
            val seen = mutableSetOf<String>()
            values.forEachIndexed { index, value ->
                val nodeId = id(value)
                if (nodeId.isNotBlank() && !seen.add(nodeId)) violation("$path[$index].id", SduiViolationCode.DuplicateSiblingIdentifier)
            }
        }

        fun template(value: TemplateDto, path: String) {
            count(path)
            bounded(value.components.size, limits.maxComponents, "$path.components")
            if (value.components.isEmpty()) violation(path, SduiViolationCode.EmptyBranch)
            validateSiblingIds(value.components, "$path.components") { it.id }
            value.components.forEachIndexed { index, item -> component(item, "$path.components[$index]") }
        }

        private fun component(value: ComponentDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.sections.size, limits.maxSectionsPerComponent, "$path.sections")
            bounded(value.elements.size, limits.maxElementsPerContainer, "$path.elements")
            validateExclusiveBranch(value.sections.isNotEmpty(), value.elements.isNotEmpty(), path)
            validateSiblingIds(value.sections, "$path.sections") { it.id }
            validateSiblingIds(value.elements, "$path.elements") { it.id }
            value.sections.forEachIndexed { index, item -> section(item, "$path.sections[$index]") }
            value.elements.forEachIndexed { index, item -> element(item, "$path.elements[$index]") }
        }

        private fun section(value: SectionDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.groups.size, limits.maxGroupsPerSection, "$path.groups")
            bounded(value.elements.size, limits.maxElementsPerContainer, "$path.elements")
            validateExclusiveBranch(value.groups.isNotEmpty(), value.elements.isNotEmpty(), path)
            validateSiblingIds(value.groups, "$path.groups") { it.id }
            validateSiblingIds(value.elements, "$path.elements") { it.id }
            value.groups.forEachIndexed { index, item -> group(item, "$path.groups[$index]") }
            value.elements.forEachIndexed { index, item -> element(item, "$path.elements[$index]") }
        }

        private fun group(value: GroupDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.elements.size, limits.maxElementsPerContainer, "$path.elements")
            if (value.elements.isEmpty()) violation(path, SduiViolationCode.MissingTerminalElements)
            validateSiblingIds(value.elements, "$path.elements") { it.id }
            value.elements.forEachIndexed { index, item -> element(item, "$path.elements[$index]") }
        }

        private fun element(value: ElementDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            value.command?.let { command(it, "$path.command") }
        }

        private fun command(value: CommandDto, path: String) {
            when (value) {
                is RequestCommandDto -> requestCommand(value, path)
                is CapabilityCommandDto -> capabilityCommand(value, path)
                is NavigationCommandDto -> navigationCommand(value, path)
                is PresentationCommandDto -> presentationCommand(value, path)
                is LocalStateCommandDto -> bounded(value.values.size, limits.maxCommandPayloadFields, "$path.values")
                is FormCommandDto -> allowed(value.operation, FORM_OPERATIONS, "$path.operation")
                is BackgroundCommandDto -> backgroundCommand(value, path)
            }
        }

        private fun requestCommand(value: RequestCommandDto, path: String) {
            if (value.method.uppercase() !in REQUEST_METHODS) violation("$path.method", SduiViolationCode.UnsupportedRequestMethod)
            when {
                value.endpoint.isBlank() -> violation("$path.endpoint", SduiViolationCode.MissingEndpoint)
                value.endpoint.length > limits.maxEndpointLength -> violation("$path.endpoint", SduiViolationCode.EndpointTooLong)
                !value.endpoint.startsWith("/") || value.endpoint.startsWith("//") || "://" in value.endpoint -> violation("$path.endpoint", SduiViolationCode.EndpointMustBeRelative)
            }
            allowed(value.authentication, REQUEST_AUTHENTICATION, "$path.authentication")
            allowed(value.responseMode, RESPONSE_MODES, "$path.responseMode")
            allowed(value.transition, TRANSITIONS, "$path.transition")
            if (value.responseMode.uppercase() == "SCREEN") {
                val screenId = value.screenId
                val templateId = value.templateId
                val templateType = value.templateType
                if (screenId == null) violation("$path.screenId", SduiViolationCode.MissingIdentifier) else identifier(screenId, "$path.screenId")
                if (templateId == null) violation("$path.templateId", SduiViolationCode.MissingIdentifier) else identifier(templateId, "$path.templateId")
                if (templateType == null) violation("$path.templateType", SduiViolationCode.MissingType) else type(templateType, "$path.templateType")
            } else {
                optionalIdentifier(value.screenId, "$path.screenId")
                optionalIdentifier(value.templateId, "$path.templateId")
                optionalType(value.templateType, "$path.templateType")
            }
            optionalIdentifier(value.backStackKey, "$path.backStackKey")
            bounded(value.payload.size, limits.maxCommandPayloadFields, "$path.payload")
        }

        private fun capabilityCommand(value: CapabilityCommandDto, path: String) {
            type(value.capability, "$path.capability")
            type(value.operation, "$path.operation")
            bounded(value.arguments.size, limits.maxCommandPayloadFields, "$path.arguments")
        }

        private fun navigationCommand(value: NavigationCommandDto, path: String) {
            allowed(value.operation, NAVIGATION_OPERATIONS, "$path.operation")
            if (value.operation.uppercase() == "POP_TO") {
                val target = value.targetNavigationId
                if (target == null) violation("$path.targetNavigationId", SduiViolationCode.MissingIdentifier)
                else identifier(target, "$path.targetNavigationId")
            }
        }

        private fun presentationCommand(value: PresentationCommandDto, path: String) {
            allowed(value.operation, PRESENTATION_OPERATIONS, "$path.operation")
            allowed(value.presentationKind, PRESENTATION_KINDS, "$path.presentationKind")
            identifier(value.id, "$path.id")
            bounded(value.properties.size, limits.maxCommandPayloadFields, "$path.properties")
        }

        private fun backgroundCommand(value: BackgroundCommandDto, path: String) {
            allowed(value.operation, BACKGROUND_OPERATIONS, "$path.operation")
            identifier(value.id, "$path.id")
            allowed(value.workKind, BACKGROUND_WORK_KINDS, "$path.workKind")
            allowed(value.networkRequirement, NETWORK_REQUIREMENTS, "$path.networkRequirement")
            if (value.earliestStartDelayMillis < 0L) violation("$path.earliestStartDelayMillis", SduiViolationCode.InvalidCommandPayload)
            bounded(value.input.size, limits.maxCommandPayloadFields, "$path.input")
            if (value.operation.uppercase() == "START_CONTINUOUS") {
                if (value.title.isNullOrBlank()) violation("$path.title", SduiViolationCode.InvalidCommandPayload)
                if (value.description.isNullOrBlank()) violation("$path.description", SduiViolationCode.InvalidCommandPayload)
            }
        }

        private fun allowed(value: String, allowed: Set<String>, path: String) {
            if (value.uppercase() !in allowed) violation(path, SduiViolationCode.UnsupportedCommandValue)
        }

        private fun validateExclusiveBranch(hasIntermediate: Boolean, hasElements: Boolean, path: String) {
            when {
                hasIntermediate && hasElements -> violation(path, SduiViolationCode.ConflictingBranchContent)
                !hasIntermediate && !hasElements -> violation(path, SduiViolationCode.EmptyBranch)
            }
        }

        private fun violation(path: String, code: SduiViolationCode) {
            violations += SduiViolation(path, code)
        }

        companion object {
            private val REQUEST_METHODS = setOf("GET", "POST", "PUT", "PATCH", "DELETE")
            private val REQUEST_AUTHENTICATION = setOf("NONE", "SESSION", "OPTIONAL_SESSION")
            private val RESPONSE_MODES = setOf("SCREEN", "NONE")
            private val TRANSITIONS = setOf("PUSH", "REPLACE", "RESET", "STAY")
            private val NAVIGATION_OPERATIONS = setOf("POP", "POP_TO")
            private val PRESENTATION_OPERATIONS = setOf("SHOW", "DISMISS")
            private val PRESENTATION_KINDS = setOf("MESSAGE", "DIALOG", "SHEET")
            private val FORM_OPERATIONS = setOf("VALIDATE", "RESET")
            private val BACKGROUND_OPERATIONS = setOf("SCHEDULE", "CANCEL", "START_CONTINUOUS", "STOP_CONTINUOUS")
            private val BACKGROUND_WORK_KINDS = setOf("REFRESH", "PROCESSING")
            private val NETWORK_REQUIREMENTS = setOf("NOT_REQUIRED", "CONNECTED", "UNMETERED")
        }
    }
}
