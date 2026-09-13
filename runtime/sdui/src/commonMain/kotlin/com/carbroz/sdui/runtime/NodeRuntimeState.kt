package com.carbroz.sdui.runtime

import kotlinx.serialization.json.JsonElement

data class NodeRuntimeState(
    val visible: Boolean? = null,
    val enabled: Boolean? = null,
    val selected: Boolean? = null,
    val expanded: Boolean? = null,
    val checked: Boolean? = null,
    val loading: Boolean? = null,
    val value: JsonElement? = null,
)
