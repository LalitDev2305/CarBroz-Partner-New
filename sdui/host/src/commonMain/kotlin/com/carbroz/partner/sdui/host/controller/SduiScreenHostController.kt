package com.carbroz.partner.sdui.host.controller

import com.carbroz.partner.core.navigation.NavCommand
import com.carbroz.partner.core.navigation.Router
import com.carbroz.partner.engine.execution.dispatcher.ActionDispatcher
import com.carbroz.partner.sdui.engine.processor.SduiProcessor
import com.carbroz.partner.sdui.engine.result.SduiParseResult
import com.carbroz.partner.sdui.host.model.SduiHostState
import com.carbroz.partner.sdui.host.repository.SduiScreenRepository
import com.carbroz.partner.sdui.render.runtime.event.SduiUiEvent
import com.carbroz.partner.sdui.runtime.SduiRuntime
import com.carbroz.partner.sdui.runtime.event.SduiRuntimeEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

public class SduiScreenHostController(
    private val scope: CoroutineScope,
    private val endpoint: String,
    private val repository: SduiScreenRepository,
    private val actionDispatcher: ActionDispatcher,
    private val router: Router? = null,
    private val processor: SduiProcessor = SduiProcessor()
) {
    private val _hostState = MutableStateFlow<SduiHostState>(SduiHostState.Loading)
    public val hostState: StateFlow<SduiHostState> get() = _hostState

    public var runtime: SduiRuntime? = null
        private set

    init {
        loadScreen(endpoint)
    }

    public fun loadScreen(targetEndpoint: String) {
        _hostState.value = SduiHostState.Loading
        scope.launch {
            repository.fetchScreenJson(targetEndpoint)
                .onSuccess { json ->
                    when (val result = processor.process(json)) {
                        is SduiParseResult.Success -> {
                            val activeRuntime = SduiRuntime(result.assembledScreen, actionDispatcher)
                            runtime = activeRuntime
                            _hostState.value = SduiHostState.Content(result.assembledScreen)
                            observeRuntimeEffects(activeRuntime)
                        }
                        is SduiParseResult.Failure -> {
                            _hostState.value = SduiHostState.Error("SDUI validation error: ${result.error}")
                        }
                    }
                }
                .onFailure { error ->
                    _hostState.value = SduiHostState.Error("Network error: ${error.message}")
                }
        }
    }

    private fun handleBackRequested() {
        val currentContent = _hostState.value as? SduiHostState.Content ?: run {
            scope.launch { router?.execute(NavCommand.Pop) }
            return
        }
        val backPolicy = currentContent.assembledScreen.screen.back

        if (backPolicy != null) {
            _hostState.value = currentContent.copy(isRefreshing = true)
            scope.launch {
                repository.fetchScreenJson(backPolicy.api)
                    .onSuccess { json ->
                        when (val result = processor.process(json)) {
                            is SduiParseResult.Success -> {
                                val activeRuntime = SduiRuntime(result.assembledScreen, actionDispatcher)
                                runtime = activeRuntime
                                _hostState.value = SduiHostState.Content(result.assembledScreen, isRefreshing = false)
                                observeRuntimeEffects(activeRuntime)
                            }
                            is SduiParseResult.Failure -> {
                                _hostState.value = currentContent.copy(isRefreshing = false)
                            }
                        }
                    }
                    .onFailure {
                        _hostState.value = currentContent.copy(isRefreshing = false)
                    }
            }
        } else {
            scope.launch { router?.execute(NavCommand.Pop) }
        }
    }

    private fun observeRuntimeEffects(activeRuntime: SduiRuntime) {
        scope.launch {
            activeRuntime.effects.collect { effect ->
                when (effect) {
                    is SduiRuntimeEffect.ScreenTransition -> {
                        loadScreen(effect.api)
                    }
                    is SduiRuntimeEffect.BackRequested -> {
                        handleBackRequested()
                    }
                    is SduiRuntimeEffect.RefreshRequested -> {
                        refreshScreen()
                    }
                    is SduiRuntimeEffect.ActionFailed -> {
                        // Action failure preserves current screen
                    }
                }
            }
        }
    }

    public fun refreshScreen() {
        val currentContent = _hostState.value as? SduiHostState.Content ?: return
        _hostState.value = currentContent.copy(isRefreshing = true)
        scope.launch {
            repository.fetchScreenJson(endpoint)
                .onSuccess { json ->
                    when (val result = processor.process(json)) {
                        is SduiParseResult.Success -> {
                            val activeRuntime = SduiRuntime(result.assembledScreen, actionDispatcher)
                            runtime = activeRuntime
                            _hostState.value = SduiHostState.Content(result.assembledScreen, isRefreshing = false)
                            observeRuntimeEffects(activeRuntime)
                        }
                        is SduiParseResult.Failure -> {
                            _hostState.value = currentContent.copy(isRefreshing = false)
                        }
                    }
                }
                .onFailure {
                    _hostState.value = currentContent.copy(isRefreshing = false)
                }
        }
    }

    public fun onUiEvent(event: SduiUiEvent) {
        scope.launch {
            runtime?.onEvent(event)
        }
    }
}
