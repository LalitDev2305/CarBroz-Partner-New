package com.carbroz.partner.sdui.render.runtime.snapshot

public data class SduiRenderSnapshot(
    val inputValues: Map<String, String> = emptyMap(),
    val validationErrors: Map<String, String> = emptyMap(),
    val executingNodeIds: Set<String> = emptySet(),
    val nodeVisibility: Map<String, Boolean> = emptyMap(),
    val nodeEnabled: Map<String, Boolean> = emptyMap(),
    val parentSignalVersions: Map<String, Long> = emptyMap()
) {
    public val nodeValues: Map<String, String> get() = inputValues

    public fun getInputValue(nodeId: String): String? = inputValues[nodeId]

    public fun getNodeValue(nodeId: String): String? = inputValues[nodeId]

    public fun getValidationError(nodeId: String): String? = validationErrors[nodeId]

    public fun isNodeExecuting(nodeId: String): Boolean = executingNodeIds.contains(nodeId)

    public fun isNodeVisible(nodeId: String, defaultVisible: Boolean): Boolean {
        return nodeVisibility[nodeId] ?: defaultVisible
    }

    public fun isNodeEnabled(nodeId: String, defaultEnabled: Boolean): Boolean {
        return nodeEnabled[nodeId] ?: defaultEnabled
    }

    public fun getParentSignalVersion(nodeId: String): Long {
        return parentSignalVersions[nodeId] ?: 0L
    }
}
