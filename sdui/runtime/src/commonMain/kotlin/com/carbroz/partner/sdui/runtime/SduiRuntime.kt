package com.carbroz.partner.sdui.runtime

import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.sdui.engine.assembly.AssembledSduiScreen
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.runtime.event.SduiRuntimeEffect
import com.carbroz.partner.sdui.runtime.event.SduiRuntimeEventHandler
import com.carbroz.partner.sdui.runtime.state.SduiScreenState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

public class SduiRuntime(
    assembledScreen: AssembledSduiScreen,
    actionDispatcher: ActionDispatcher
) {
    private val _state = MutableStateFlow(SduiScreenState(assembledScreen = assembledScreen))
    public val state: StateFlow<SduiScreenState> get() = _state

    private val _effects = MutableSharedFlow<SduiRuntimeEffect>(replay = 1, extraBufferCapacity = 64)
    public val effects: SharedFlow<SduiRuntimeEffect> get() = _effects

    private val handler = SduiRuntimeEventHandler(actionDispatcher)

    public suspend fun onEvent(event: SduiUiEvent) {
        _state.value = handler.handleEvent(event, _state.value) { effect ->
            _effects.tryEmit(effect)
        }
    }
}
