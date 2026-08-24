package com.carbroz.runtime.sdui.interaction

import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.ComponentContent
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.SectionContent
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent

/**
 * Immutable lookup prepared from trusted normalized IR. Renderers emit only a canonical [NodePath];
 * command lookup stays outside Compose element implementations and remains O(1) during interaction.
 */
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
        fun from(screen: Screen): SduiCommandIndex {
            val commands = buildMap {
                screen.template.components.forEach { component ->
                    when (val content = component.content) {
                        is ComponentContent.Elements -> content.values.forEach { element ->
                            element.command?.let { put(element.path, it) }
                        }

                        is ComponentContent.Sections -> content.values.forEach { section ->
                            when (val sectionContent = section.content) {
                                is SectionContent.Elements -> sectionContent.values.forEach { element ->
                                    element.command?.let { put(element.path, it) }
                                }

                                is SectionContent.Groups -> sectionContent.values.forEach { group ->
                                    group.elements.forEach { element ->
                                        element.command?.let { put(element.path, it) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return SduiCommandIndex(commands)
        }
    }
}
