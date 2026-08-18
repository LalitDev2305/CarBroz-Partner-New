package com.carbroz.partner.sdui.render.runtime.snapshot

public data class SduiRenderSnapshot(
    val inputValues: Map<String, String> = emptyMap(),
    val executingNodeIds: Set<String> = emptySet(),
    val nodeVisibility: Map<String, Boolean> = emptyMap(),
    val nodeEnabled: Map<String, Boolean> = emptyMap(),
    val timerStates: Map<String, TimerPresentationState> = emptyMap()
) {
    public fun isNodeExecuting(nodeId: String): Boolean = executingNodeIds.contains(nodeId)
    public fun isNodeVisible(nodeId: String, defaultVisible: Boolean): Boolean =
        nodeVisibility[nodeId] ?: defaultVisible
    public fun isNodeEnabled(nodeId: String, defaultEnabled: Boolean): Boolean =
        if (isNodeExecuting(nodeId)) false else (nodeEnabled[nodeId] ?: defaultEnabled)
    public fun getInputValue(nodeId: String): String? = inputValues[nodeId]
    public fun getTimerState(nodeId: String): TimerPresentationState? = timerStates[nodeId]
}

public data class TimerPresentationState(
    val remainingSeconds: Int,
    val isRunning: Boolean
)
