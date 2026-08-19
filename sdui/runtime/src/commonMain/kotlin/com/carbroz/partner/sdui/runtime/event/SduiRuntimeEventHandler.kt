package com.carbroz.partner.sdui.runtime.event

import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.engine.execution.result.ExecutionResult
import com.carbroz.partner.sdui.engine.model.SduiAction
import com.carbroz.partner.sdui.engine.model.SduiChildrenData
import com.carbroz.partner.sdui.engine.model.SduiParentAction
import com.carbroz.partner.sdui.render.renderer.childdata.input.InputChildrenDataProperties
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.runtime.bridge.SduiActionMapper
import com.carbroz.partner.sdui.runtime.bridge.SduiBindingScopeAdapter
import com.carbroz.partner.sdui.runtime.handler.ParentActionHandler
import com.carbroz.partner.sdui.runtime.state.SduiScreenState
import com.carbroz.partner.sdui.runtime.validation.SduiValidationResolver

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

@Suppress("DEPRECATION")
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
            is SduiUiEvent.ValueChanged -> {
                handleValueEmitted(event.nodeId, event.newValue, currentState)
            }
            is SduiUiEvent.InputChanged -> {
                handleValueEmitted(event.nodeId, event.newValue, currentState)
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

    private fun handleValueEmitted(
        nodeId: String,
        newValue: String,
        currentState: SduiScreenState
    ): SduiScreenState {
        val updatedValues = currentState.overlay.nodeValues + (nodeId to newValue)
        // Evaluate validation rules if node is present in nodeIndex
        val targetNode = currentState.assembledScreen.nodeIndex[nodeId]
        val updatedErrors = if (targetNode is SduiChildrenData) {
            val props = InputChildrenDataProperties.decode(targetNode.properties)
            val error = SduiValidationResolver.validate(newValue, props.validationRules)
            if (error != null) {
                currentState.overlay.validationErrors + (nodeId to error)
            } else {
                currentState.overlay.validationErrors - nodeId
            }
        } else {
            currentState.overlay.validationErrors
        }

        return currentState.copy(
            overlay = currentState.overlay.copy(
                nodeValues = updatedValues,
                validationErrors = updatedErrors
            )
        )
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
            val scope = SduiBindingScopeAdapter(state.overlay.nodeValues)
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
