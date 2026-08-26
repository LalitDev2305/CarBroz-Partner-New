package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.runtime.action.ActionPreparationContext
import com.carbroz.runtime.action.ActionPreparationResult
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.binding.BindingNamespace
import com.carbroz.runtime.binding.BindingValueSource
import com.carbroz.runtime.sdui.SduiPipelineResult
import com.carbroz.runtime.sdui.model.Command
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

data class DynamicScreenState(
    val destination: DynamicDestination? = null,
    val loading: Boolean = false,
    val actionInFlight: Boolean = false,
    val screen: Screen? = null,
    val failure: DynamicScreenFailure? = null,
)

sealed interface DynamicScreenFailure {
    data class Network(val detail: String) : DynamicScreenFailure
    data class Protocol(val detail: String) : DynamicScreenFailure
    data class Action(val detail: String) : DynamicScreenFailure
}

class DynamicScreenCache {
    private val screens = mutableMapOf<String, Screen>()

    fun get(destination: DynamicDestination): Screen? = screens[destination.navigationId]

    fun put(destination: DynamicDestination, screen: Screen) {
        screens[destination.navigationId] = screen
    }

    fun clear() {
        screens.clear()
    }
}

fun interface DynamicBindingContextFactory {
    suspend fun create(screen: Screen?): BindingContext
}

class DefaultDynamicBindingContextFactory : DynamicBindingContextFactory {
    override suspend fun create(screen: Screen?): BindingContext {
        val source = BindingValueSource { path ->
            when (path.joinToString(".")) {
                "id" -> screen?.let { JsonPrimitive(it.id.value) }
                "template.id" -> screen?.let { JsonPrimitive(it.template.id.value) }
                "template.type" -> screen?.let { JsonPrimitive(it.template.type.value) }
                else -> null
            }
        }
        return BindingContext.of(BindingNamespace.SCREEN to source)
    }
}

/** Generic state owner for every backend-driven screen. No business screen names are known here. */
class DynamicSduiStore(
    private val scope: CoroutineScope,
    private val runtime: ReferenceSduiRuntime,
    private val actionPreparer: ActionPreparer,
    private val networkActions: NetworkActionExecutor,
    private val capabilityActions: CapabilityActionExecutor,
    private val navigation: NavigationStore,
    private val cache: DynamicScreenCache = DynamicScreenCache(),
    private val bindingContexts: DynamicBindingContextFactory = DefaultDynamicBindingContextFactory(),
) {
    private val mutableState = MutableStateFlow(DynamicScreenState())
    val state: StateFlow<DynamicScreenState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    fun show(destination: DynamicDestination, forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            cache.get(destination)?.let { cached ->
                mutableState.value = DynamicScreenState(destination = destination, screen = cached)
                return
            }
        }

        loadJob?.cancel()
        loadJob = scope.launch {
            mutableState.value = DynamicScreenState(destination = destination, loading = true)
            val instruction = destination.instruction
            val action = PreparedAction.Request(
                method = instruction.request.method,
                endpoint = instruction.request.endpoint,
                destination = instruction.destination,
                payload = instruction.request.payload,
            )

            when (val result = networkActions.execute(action)) {
                is NetworkResult.Failure -> mutableState.update {
                    it.copy(
                        loading = false,
                        failure = DynamicScreenFailure.Network(result.error.toString()),
                    )
                }

                is NetworkResult.Success -> consume(
                    destination = destination,
                    expected = instruction.destination,
                    body = result.response.body,
                    status = result.response.statusCode,
                )
            }
        }
    }

    fun retry() {
        state.value.destination?.let { show(it, forceRefresh = true) }
    }

    fun onCommand(command: Command) {
        if (state.value.actionInFlight) return

        actionJob?.cancel()
        actionJob = scope.launch {
            mutableState.update { it.copy(actionInFlight = true, failure = null) }
            val context = ActionPreparationContext(
                bindings = bindingContexts.create(state.value.screen),
                form = null,
            )
            when (val prepared = actionPreparer.prepare(command, context)) {
                is ActionPreparationResult.Success -> execute(prepared.action)
                else -> mutableState.update {
                    it.copy(
                        actionInFlight = false,
                        failure = DynamicScreenFailure.Action(prepared.toString()),
                    )
                }
            }
        }
    }

    fun onRenderFailure(detail: String) {
        mutableState.update {
            it.copy(
                loading = false,
                actionInFlight = false,
                failure = DynamicScreenFailure.Protocol(detail),
            )
        }
    }

    private suspend fun execute(action: PreparedAction) {
        try {
            when (action) {
                is PreparedAction.Request -> when (val result = networkActions.execute(action)) {
                    is NetworkResult.Failure -> mutableState.update {
                        it.copy(
                            actionInFlight = false,
                            failure = DynamicScreenFailure.Network(result.error.toString()),
                        )
                    }

                    is NetworkResult.Success -> {
                        val next = DynamicDestination(
                            DynamicScreenInstruction(
                                destination = action.destination,
                                request = DynamicScreenRequest(
                                    method = action.method,
                                    endpoint = action.endpoint,
                                    payload = action.payload,
                                ),
                                transition = DynamicTransition.PUSH,
                            ),
                        )
                        consume(
                            destination = next,
                            expected = action.destination,
                            body = result.response.body,
                            status = result.response.statusCode,
                        )
                        if (state.value.failure == null) {
                            applyTransition(next)
                        }
                    }
                }

                is PreparedAction.Capability -> {
                    capabilityActions.execute(action)
                    mutableState.update { it.copy(actionInFlight = false) }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            mutableState.update {
                it.copy(
                    actionInFlight = false,
                    failure = DynamicScreenFailure.Action(
                        failure::class.simpleName ?: "action_failure",
                    ),
                )
            }
        }
    }

    private fun consume(
        destination: DynamicDestination,
        expected: ScreenDestination,
        body: JsonElement?,
        status: Int,
    ) {
        if (status !in 200..299) {
            mutableState.update {
                it.copy(
                    loading = false,
                    actionInFlight = false,
                    failure = DynamicScreenFailure.Network("http_$status"),
                )
            }
            return
        }

        if (body == null) {
            failProtocol("missing_response_body")
            return
        }

        when (val processed = runtime.pipeline.process(body.toString())) {
            is SduiPipelineResult.Success -> acceptScreen(destination, expected, processed.screen)
            else -> failProtocol(processed.toString())
        }
    }

    private fun acceptScreen(
        destination: DynamicDestination,
        expected: ScreenDestination,
        screen: Screen,
    ) {
        if (
            screen.id.value != expected.screenId ||
            screen.template.id.value != expected.templateId ||
            screen.template.type != expected.templateType
        ) {
            failProtocol("destination_identity_mismatch")
            return
        }

        cache.put(destination, screen)
        mutableState.value = DynamicScreenState(
            destination = destination,
            screen = screen,
        )
    }

    private fun failProtocol(detail: String) {
        mutableState.update {
            it.copy(
                loading = false,
                actionInFlight = false,
                failure = DynamicScreenFailure.Protocol(detail),
            )
        }
    }

    private fun applyTransition(destination: DynamicDestination) {
        when (destination.instruction.transition) {
            DynamicTransition.PUSH -> navigation.dispatch(NavigationCommand.Push(destination))
            DynamicTransition.REPLACE -> navigation.dispatch(NavigationCommand.ReplaceTop(destination))
            DynamicTransition.RESET -> navigation.dispatch(NavigationCommand.ResetTo(destination))
            DynamicTransition.STAY -> Unit
        }
    }

    fun close() {
        loadJob?.cancel()
        actionJob?.cancel()
    }
}
