package com.carbroz.partner.sdui.engine.result

import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen

public sealed interface SduiParseResult {
    public data class Success(
        val assembledScreen: AssembledSduiScreen,
        val warnings: List<String> = emptyList()
    ) : SduiParseResult

    public data class Failure(
        val error: SduiParseError
    ) : SduiParseResult
}

public sealed interface SduiParseError {
    val code: String
    val message: String

    public data class MalformedJson(override val message: String) : SduiParseError {
        override val code: String = "MALFORMED_JSON"
    }

    public data class MissingRequiredField(override val message: String) : SduiParseError {
        override val code: String = "MISSING_REQUIRED_FIELD"
    }

    public data class InvalidHierarchy(override val message: String) : SduiParseError {
        override val code: String = "INVALID_HIERARCHY"
    }

    public data class UnsupportedSchemaVersion(override val message: String) : SduiParseError {
        override val code: String = "UNSUPPORTED_SCHEMA_VERSION"
    }

    public data class InvalidPropertyValue(override val message: String) : SduiParseError {
        override val code: String = "INVALID_PROPERTY_VALUE"
    }

    public data class DuplicateNodeId(override val message: String) : SduiParseError {
        override val code: String = "DUPLICATE_NODE_ID"
    }

    public data class InvalidParentActionTarget(override val message: String) : SduiParseError {
        override val code: String = "INVALID_PARENT_ACTION_TARGET"
    }

    public data class TargetDoesNotAcceptParentAction(override val message: String) : SduiParseError {
        override val code: String = "TARGET_DOES_NOT_ACCEPT_PARENT_ACTION"
    }

    public data class IncompleteTemplateTransition(override val message: String) : SduiParseError {
        override val code: String = "INCOMPLETE_TEMPLATE_TRANSITION"
    }

    public data class PayloadLimitExceeded(override val message: String) : SduiParseError {
        override val code: String = "PAYLOAD_LIMIT_EXCEEDED"
    }

    public data class UnsupportedActionNodeType(override val message: String) : SduiParseError {
        override val code: String = "UNSUPPORTED_ACTION_NODE_TYPE"
    }
}
