package com.carbroz.runtime.sdui.interaction

import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.elements
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent

/** Immutable O(1) command lookup built from the canonical typed SDUI traversal. */
class SduiCommandIndex private constructor(
    private val commands: Map<NodePath, Command>,
) {
    fun commandFor(event: SduiRenderEvent): Command? = when (event) {
        is SduiRenderEvent.Activated -> commands[event.path]
        is SduiRenderEvent.ValueChanged -> commands[event.path]
    }

    fun commandAt(path: NodePath): Command? = commands[path]
    val size: Int get() = commands.size

    companion object {
        fun from(screen: Screen): SduiCommandIndex = SduiCommandIndex(
            screen.elements().mapNotNull { element -> element.command?.let { element.path to it } }.toMap(),
        )
    }
}
