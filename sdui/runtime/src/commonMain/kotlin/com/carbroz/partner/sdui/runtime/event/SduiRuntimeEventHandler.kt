package com.carbroz.partner.sdui.runtime.event

import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.runtime.bridge.SduiActionMapper
import com.carbroz.partner.sdui.runtime.bridge.SduiBindingScopeAdapter
import com.carbroz.partner.sdui.runtime.handler.ParentActionHandler
import com.carbroz.partner.sdui.runtime.state.SduiScreenState

public sealed interface SduiRuntimeEffect {
    public data class ScreenTransition(
        val api: String,
        val templateId: String?,
        val templateType: String?,
        val resultOutput: String?
    ) : SduiRuntimeEffect

    public data class ActionFailed(
        val message: String
    ) : SduiRuntimeEffect

    public object BackRequested : SduiRuntimeEffect
    public object RefreshRequested : SduiRuntimeEffect
}

public class SduiRuntimeEventHandler(
    private val actionDispatcher: ActionDispatcher,
    private val actionMapper: SduiActionMapper = SduiActionMapper(),
    private val parentActionHandler: ParentActionHandler = ParentActionHandler()
) {
    public suspend fun handleEvent(
        event: SduiUiEvent,
        currentState: SduiScreenState,
        onEffect: (SduiRuntimeEffect) -> Unit
    ): SduiScreenState {
        return when (event) {
            is SduiUiEvent.InputChanged -> {
                val updatedInputs = currentState.overlay.inputValues + (event.nodeId to event.newValue)
                currentState.copy(overlay = currentState.overlay.copy(inputValues = updatedInputs))
            }
            is SduiUiEvent.NodeTriggered -> {
                handleNodeTriggered(event.node.id, event.action, event.parentAction, currentState, onEffect)
            }
            is SduiUiEvent.BackRequested -> {
                onEffect(SduiRuntimeEffect.BackRequested)
                currentState
            }
            is SduiUiEvent.RefreshRequested -> {
                onEffect(SduiRuntimeEffect.RefreshRequested)
                currentState
            }
        }
    }

    private suspend fun handleNodeTriggered(
        nodeId: String,
        action: SduiAction?,
        parentAction: SduiParentAction?,
        currentState: SduiScreenState,
        onEffect: (SduiRuntimeEffect) -> Unit
    ): SduiScreenState {
        var state = currentState

        if (action != null) {
            val scope = SduiBindingScopeAdapter(state.overlay.inputValues)
            val actionSpec = actionMapper.mapToActionSpec(nodeId, action)
            val result = actionDispatcher.dispatch(actionSpec, scope)

            when (result) {
                is ExecutionResult.Success -> {
                    if (parentAction != null) {
                        state = state.copy(
                            overlay = parentActionHandler.handle(parentAction, state.assembledScreen, state.overlay)
                        )
                    }
                    onEffect(
                        SduiRuntimeEffect.ScreenTransition(
                            api = action.api,
                            templateId = action.templateId,
                            templateType = action.templateType,
                            resultOutput = result.output.toString()
                        )
                    )
                }
                is ExecutionResult.Failure -> {
                    onEffect(SduiRuntimeEffect.ActionFailed(result.failure.message))
                }
            }
        } else if (parentAction != null) {
            state = state.copy(
                overlay = parentActionHandler.handle(parentAction, state.assembledScreen, state.overlay)
            )
        }

        return state
    }
}
