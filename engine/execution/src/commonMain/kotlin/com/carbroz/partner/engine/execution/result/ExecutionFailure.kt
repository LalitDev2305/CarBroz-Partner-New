package com.carbroz.partner.engine.execution.result

/**
 * Structured failure descriptor for action execution operations.
 */
data class ExecutionFailure(
    val code: FailureCode,
    val message: String
) {
    init {
        require(message.isNotBlank()) { "ExecutionFailure message must not be blank" }
    }

    enum class FailureCode {
        UNREGISTERED_ACTION_TYPE,
        BINDING_RESOLUTION_FAILED,
        EXECUTOR_FAILED,
        UNKNOWN
    }
}
