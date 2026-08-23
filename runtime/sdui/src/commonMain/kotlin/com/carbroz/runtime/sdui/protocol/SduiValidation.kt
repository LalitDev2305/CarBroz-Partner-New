package com.carbroz.runtime.sdui.protocol

data class SduiProtocolLimits(
    val maxPayloadCharacters: Int = 1_000_000,
    val maxIdentifierLength: Int = 128,
    val maxTypeLength: Int = 128,
    val maxRequiredIdentifiers: Int = 128,
    val maxComponents: Int = 256,
    val maxSubComponentsPerComponent: Int = 128,
    val maxChildrenPerContainer: Int = 256,
    val maxChildDataPerChild: Int = 128,
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
    DuplicateIdentifier,
    EmptyBranch,
    MissingTerminalData,
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
        context.bounded(envelope.requiredRenderers.size, limits.maxRequiredIdentifiers, "requiredRenderers")
        context.bounded(envelope.requiredCapabilities.size, limits.maxRequiredIdentifiers, "requiredCapabilities")
        envelope.requiredRenderers.forEachIndexed { index, value -> context.type(value, "requiredRenderers[$index]") }
        envelope.requiredCapabilities.forEachIndexed { index, value -> context.type(value, "requiredCapabilities[$index]") }
        context.template(envelope.screen.template, "screen.template")
        return if (context.violations.isEmpty()) SduiValidationResult.Valid
        else SduiValidationResult.Invalid(context.violations.toList())
    }

    private class ValidationContext(private val limits: SduiProtocolLimits) {
        val violations = mutableListOf<SduiViolation>()
        private val ids = mutableSetOf<String>()
        private var nodes = 0

        fun identifier(value: String, path: String) {
            if (value.isBlank()) violation(path, SduiViolationCode.MissingIdentifier)
            else {
                if (value.length > limits.maxIdentifierLength) violation(path, SduiViolationCode.IdentifierTooLong)
                if (!ids.add(value)) violation(path, SduiViolationCode.DuplicateIdentifier)
            }
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

        fun template(value: TemplateDto, path: String) {
            count(path)
            bounded(value.components.size, limits.maxComponents, "$path.components")
            bounded(value.children.size, limits.maxChildrenPerContainer, "$path.children")
            if (value.components.isEmpty() && value.children.isEmpty()) violation(path, SduiViolationCode.EmptyBranch)
            value.components.forEachIndexed { index, component -> component(component, "$path.components[$index]") }
            value.children.forEachIndexed { index, child -> child(child, "$path.children[$index]") }
        }

        private fun component(value: ComponentDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.subComponents.size, limits.maxSubComponentsPerComponent, "$path.subComponents")
            bounded(value.children.size, limits.maxChildrenPerContainer, "$path.children")
            if (value.subComponents.isEmpty() && value.children.isEmpty()) violation(path, SduiViolationCode.EmptyBranch)
            value.subComponents.forEachIndexed { index, item -> subComponent(item, "$path.subComponents[$index]") }
            value.children.forEachIndexed { index, item -> child(item, "$path.children[$index]") }
        }

        private fun subComponent(value: SubComponentDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.children.size, limits.maxChildrenPerContainer, "$path.children")
            if (value.children.isEmpty()) violation(path, SduiViolationCode.EmptyBranch)
            value.children.forEachIndexed { index, item -> child(item, "$path.children[$index]") }
        }

        private fun child(value: ChildDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
            bounded(value.data.size, limits.maxChildDataPerChild, "$path.data")
            if (value.data.isEmpty()) violation(path, SduiViolationCode.MissingTerminalData)
            value.data.forEachIndexed { index, item -> childData(item, "$path.data[$index]") }
        }

        private fun childData(value: ChildDataDto, path: String) {
            count(path)
            identifier(value.id, "$path.id")
            type(value.type, "$path.type")
        }

        private fun violation(path: String, code: SduiViolationCode) {
            violations += SduiViolation(path, code)
        }
    }
}
