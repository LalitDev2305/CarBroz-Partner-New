package com.carbroz.partner.sdui.runtime.state

public data class SduiNodeOverlay(
    val inputValues: Map<String, String> = emptyMap(),
    val executingNodeIds: Set<String> = emptySet(),
    val nodeVisibility: Map<String, Boolean> = emptyMap(),
    val nodeEnabled: Map<String, Boolean> = emptyMap(),
    val parentSignalVersions: Map<String, Long> = emptyMap()
)
