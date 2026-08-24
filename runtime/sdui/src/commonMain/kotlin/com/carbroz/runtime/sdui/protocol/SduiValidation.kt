package com.carbroz.runtime.sdui.protocol

data class SduiProtocolLimits(
    val maxPayloadCharacters: Int = 1_000_000,
    val maxIdentifierLength: Int = 128,
    val maxTypeLength: Int = 128,
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

data class SduiViolation(
    val path: String,
    val code: SduiViolationCode,
)

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
}

/** Strict structural validator for untrusted transport data. */
class SduiSchemaValidator(
    private val limits: SduiProtocolLimits = SduiProtocolLimits(),
) {
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
        return if (context.violations.isEmpty()) SduiValidationResult.Valid
        else SduiValidationResult.Invalid(context.violations.toList())
    }

    private class ValidationContext(private val limits: SduiProtocolLimits) {
        val violations = mutableListOf<SduiViolation>()
        private var nodes = 0

        fun identifier(value: String, path: String) {
            if (value.isBlank()) violation(path, SduiViolationCode.MissingIdentifier)
            else if (value.length > limits.maxIdentifierLength) violation(path, SduiViolationCode.IdentifierTooLong)
        }

        fun type(value: String, path: String) {
            if (value.isBlank()) violation(path, SduiViolationCode.MissingType)
            else if (value.length > limits.maxTypeLength) violation(path, SduiViolationCode.TypeTooLong)
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
                if (nodeId.isNotBlank() && !seen.add(nodeId)) {
                    violation("$path[$index].id", SduiViolationCode.DuplicateSiblingIdentifier)
                }
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
    }
}
