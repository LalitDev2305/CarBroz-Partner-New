package com.carbroz.runtime.sdui.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.carbroz.runtime.sdui.interaction.SduiCommandIndex
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.NodePath
import com.carbroz.runtime.sdui.model.Screen

data class SduiCommandIntent(
    val path: NodePath,
    val command: Command,
    val event: SduiRenderEvent,
)

/** Renders one already validated and normalized SDUI screen; feature lifecycle is owned elsewhere. */
@Composable
fun SduiScreenRenderer(
    screen: Screen,
    dispatcher: SduiRendererDispatcher,
    onCommand: (SduiCommandIntent) -> Unit,
    onRenderFailure: (SduiRenderFailure) -> Unit,
    values: SduiRuntimeValueSource = SduiRuntimeValueSource.Empty,
) {
    val commandIndex = remember(screen) { SduiCommandIndex.from(screen) }
    val context = remember(screen, onCommand, values) {
        SduiRenderContext(
            events = SduiEventSink { event ->
                commandIndex.commandFor(event)?.let { command ->
                    onCommand(SduiCommandIntent(event.path, command, event))
                }
            },
            values = values,
        )
    }

    dispatcher.RenderScreen(screen = screen, context = context, onFailure = onRenderFailure)
}
