package com.carbroz.runtime.sdui.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.carbroz.runtime.sdui.interaction.SduiCommandIndex
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.Screen

/**
 * Semantic command intent emitted out of the rendering layer. Execution belongs to the owning MVI/runtime.
 */
data class SduiCommandIntent(
    val path: NodePath,
    val command: Command,
    val event: SduiRenderEvent,
)

/**
 * Canonical Compose host for an already validated and normalized SDUI screen.
 * It renders through the centralized dispatcher and translates UI-only events into command intents.
 */
@Composable
fun DynamicScreenHost(
    screen: Screen,
    dispatcher: SduiRendererDispatcher,
    onCommand: (SduiCommandIntent) -> Unit,
    onRenderFailure: (SduiRenderFailure) -> Unit,
) {
    val commandIndex = remember(screen) { SduiCommandIndex.from(screen) }
    val context = remember(screen, onCommand) {
        SduiRenderContext(
            events = SduiEventSink { event ->
                commandIndex.commandFor(event)?.let { command ->
                    onCommand(
                        SduiCommandIntent(
                            path = event.path,
                            command = command,
                            event = event,
                        ),
                    )
                }
            },
        )
    }

    dispatcher.RenderScreen(
        screen = screen,
        context = context,
        onFailure = onRenderFailure,
    )
}
