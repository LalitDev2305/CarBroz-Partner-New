package com.carbroz.partner.core.observability.model

/**
 * Immutable correlation context propagating diagnostic metadata across asynchronous boundaries.
 */
data class TraceContext(
    val traceId: String,
    val operationId: String? = null,
    val requestId: String? = null,
    val screenId: String? = null,
    val actionId: String? = null,
    val workflowId: String? = null
) {
    /**
     * Merge this base context with an operation-specific [override] context.
     * Non-null fields in [override] take precedence over this context.
     */
    fun merge(override: TraceContext?): TraceContext {
        if (override == null) return this
        return TraceContext(
            traceId = override.traceId.ifEmpty { this.traceId },
            operationId = override.operationId ?: this.operationId,
            requestId = override.requestId ?: this.requestId,
            screenId = override.screenId ?: this.screenId,
            actionId = override.actionId ?: this.actionId,
            workflowId = override.workflowId ?: this.workflowId
        )
    }
}

