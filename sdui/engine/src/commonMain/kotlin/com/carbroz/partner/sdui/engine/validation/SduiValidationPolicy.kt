package com.carbroz.partner.sdui.engine.validation

public data class SduiValidationPolicy(
    val maxPayloadSizeBytes: Int = 524288,
    val maxHierarchyDepth: Int = 6,
    val maxNodesPerScreen: Int = 200,
    val maxStringLength: Int = 1000,
    val supportedSchemaVersion: Int = 1
)
