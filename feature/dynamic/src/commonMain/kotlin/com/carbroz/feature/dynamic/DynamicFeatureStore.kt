package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.runtime.action.ActionPreparationContext
import com.carbroz.runtime.action.ActionPreparationResult
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.sdui.SduiPipelineResult
import com.carbroz.runtime.sdui.model.FormOperation
import com.carbroz.runtime.sdui.model.NavigationOperation
import com.carbroz.runtime.sdui.model.PresentationKind
import com.carbroz.runtime.sdui.model.PresentationOperation
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.RequestResponseMode
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import com.carbroz.runtime.sdui.rendering.SduiCommandIntent
import com.carbroz.runtime.sdui.rendering.SduiRenderEvent
import com.carbroz.runtime.sdui.rendering.SduiRuntimeValueSource
import com.carbroz.runtime.sdui.template.form.runtime.FormFieldId
import com.carbroz.runtime.sdui.template.form.runtime.FormState
import com.carbroz.runtime.sdui.template.form.runtime.FormStore
import com.carbroz.runtime.sdui.template.form.runtime.FormTemplateRuntimeFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class DynamicPresentationState(
    val kind: PresentationKind,
    val id: String,
    val properties: JsonObject,
)

data class DynamicScreenState(
    val destination: DynamicDestination? = null,
    val loading: Boolean = false,
    val actionInFlight: Boolean = false,
    val screen: Screen? = null,
    val form: FormStore? = null,
    val presentation: DynamicPresentationState? = null,
    val failure: DynamicScreenFailure? = null,
)

sealed interface DynamicScreenFailure {
    data class Network(val detail: String) : DynamicScreenFailure
    data class Protocol(val detail: String) : DynamicScreenFailure
    data class Action(val detail: String) : DynamicScreenFailure
    data class Restoration(val detail: String) : DynamicScreenFailure
}

data class DynamicScreenSnapshot(
    val screen: Screen,
    val formState: FormState?,
    val runtimeValues: Map<String, JsonElement>,
    val lastResult: JsonElement?,
    val lastExternalEvent: JsonElement?,
    val presentation: DynamicPresentationState?,
)

class DynamicScreenCache(private val maxEntries: Int = 12) {
    private val snapshots = linkedMapOf<String, DynamicScreenSnapshot>()

    init { require(maxEntries > 0) }

    fun get(destination: DynamicDestination): DynamicScreenSnapshot? = snapshots[destination.navigationId]
    fun put(destination: DynamicDestination, snapshot: DynamicScreenSnapshot) {
        snapshots.remove(destination.navigationId)
        snapshots[destination.navigationId] = snapshot
        while (snapshots.size > maxEntries) snapshots.remove(snapshots.keys.first())
    }
    fun clear() { snapshots.clear() }
}

/** Single feature-level MVI owner for every backend-driven screen. */
class DynamicFeatureStore(
    private val scope: CoroutineScope,
    private val runtime: DynamicSduiRuntime,
    private val actionPreparer: ActionPreparer,
    private val networkActions: NetworkActionExecutor,
    private val capabilityActions: CapabilityActionExecutor,
    private val navigation: NavigationStore,
    private val bindingContexts: DynamicBindingContextFactory,
    private val formRuntime: FormTemplateRuntimeFactory,
    private val backgroundActions: BackgroundActionExecutor,
    private val cache: DynamicScreenCache = DynamicScreenCache(),
) {
    private val mutableState = MutableStateFlow(DynamicScreenState())
    val state: StateFlow<DynamicScreenState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null
    private var currentForm: FormStore? = null
    private var runtimeValues: Map<String, JsonElement> = emptyMap()
    private var lastResult: JsonElement? = null
    private var lastExternalEvent: JsonElement? = null

    val renderValues: SduiRuntimeValueSource = SduiRuntimeValueSource { _, bindingKey ->
        bindingKey?.let { currentForm?.state?.value?.get(FormFieldId(it))?.value }
    }

    fun show(destination: DynamicDestination, forceRefresh: Boolean = false) {
        if (state.value.destination?.navigationId != destination.navigationId) snapshotCurrent()
        loadJob?.cancel()
        actionJob?.cancel()

        if (!forceRefresh && destination.instruction.restorePolicy != DynamicRestorePolicy.NETWORK_ONLY) {
            val cached = cache.get(destination)
            if (cached != null && destination.instruction.restorePolicy != DynamicRestorePolicy.REFRESH) {
                restore(destination, cached)
                return
            }
            if (cached == null && destination.instruction.restorePolicy == DynamicRestorePolicy.CACHE_ONLY) {
                mutableState.value = DynamicScreenState(
                    destination = destination,
                    failure = DynamicScreenFailure.Restoration("dynamic_screen_cache_miss"),
                )
                return
            }
        }

        loadJob = scope.launch {
            mutableState.value = DynamicScreenState(destination = destination, loading = true)
            val instruction = destination.instruction
            val action = PreparedAction.Request(
                method = instruction.request.method,
                endpoint = instruction.request.endpoint,
                destination = instruction.destination,
                payload = instruction.request.payload,
                authentication = instruction.request.authentication,
                responseMode = RequestResponseMode.SCREEN,
                transition = instruction.transition,
                backStackKey = instruction.backStackKey,
            )
            consumeScreenRequest(destination, instruction.destination, networkActions.execute(action))
        }
    }

    fun retry() { state.value.destination?.let { show(it, forceRefresh = true) } }

    fun onCommand(intent: SduiCommandIntent) {
        if (state.value.actionInFlight) return
        updateFormFromEvent(intent.event)
        actionJob?.cancel()
        actionJob = scope.launch {
            mutableState.update { it.copy(actionInFlight = true, failure = null) }
            val context = ActionPreparationContext(
                bindings = bindingContexts.create(
                    DynamicBindingSnapshot(
                        screen = state.value.screen,
                        form = currentForm,
                        event = intent.event,
                        externalEvent = lastExternalEvent,
                        result = lastResult,
                        runtimeValues = runtimeValues,
                        navigationId = state.value.destination?.navigationId,
                    ),
                ),
                form = currentForm,
            )
            when (val prepared = actionPreparer.prepare(intent.command, context)) {
                is ActionPreparationResult.Success -> execute(prepared.action)
                else -> failAction(prepared.toString())
            }
        }
    }

    fun onExternalData(values: JsonObject) {
        lastExternalEvent = values
        runtimeValues = runtimeValues + values.toMap()
        snapshotCurrent()
    }

    fun refreshFromExternalEvent() {
        val destination = state.value.destination ?: return
        if (destination.instruction.request.method == RequestMethod.GET) {
            show(destination, forceRefresh = true)
        } else {
            lastExternalEvent = JsonObject(mapOf("refreshRejected" to JsonPrimitive("non_idempotent_destination")))
        }
    }

    fun onExternalInstruction(instruction: DynamicScreenInstruction) {
        val destination = DynamicDestination(instruction)
        if (
            instruction.transition == ScreenTransition.STAY &&
            navigation.state.value.current.navigationId == destination.navigationId
        ) show(destination, forceRefresh = true) else applyTransition(destination, instruction.transition)
    }

    fun suspendForBackground() {
        snapshotCurrent()
        loadJob?.cancel()
        actionJob?.cancel()
        loadJob = null
        actionJob = null
        mutableState.update { it.copy(loading = false, actionInFlight = false) }
    }

    fun onRenderFailure(detail: String) {
        mutableState.update {
            it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Protocol(detail))
        }
    }

    fun dismissPresentation() {
        mutableState.update { it.copy(presentation = null) }
        snapshotCurrent()
    }

    private suspend fun execute(action: PreparedAction) {
        try {
            when (action) {
                is PreparedAction.Request -> executeRequest(action)
                is PreparedAction.Capability -> executeCapability(action)
                is PreparedAction.Navigation -> executeNavigation(action)
                is PreparedAction.Presentation -> executePresentation(action)
                is PreparedAction.LocalState -> {
                    runtimeValues = runtimeValues + action.values
                    mutableState.update { it.copy(actionInFlight = false) }
                    snapshotCurrent()
                }
                is PreparedAction.Form -> executeForm(action)
                is PreparedAction.Background -> executeBackground(action)
                is PreparedAction.Sequence -> executeSequence(action)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            failAction(failure::class.simpleName ?: "action_failure")
        }
    }

    private suspend fun executeSequence(sequence: PreparedAction.Sequence) {
        sequence.actions.forEach { child ->
            mutableState.update { it.copy(actionInFlight = true) }
            execute(child)
            if (state.value.failure != null) return
        }
        mutableState.update { it.copy(actionInFlight = false) }
        snapshotCurrent()
    }

    private suspend fun executeRequest(action: PreparedAction.Request) {
        when (val result = networkActions.execute(action)) {
            is NetworkResult.Failure -> mutableState.update {
                it.copy(actionInFlight = false, failure = DynamicScreenFailure.Network(result.error.toString()))
            }
            is NetworkResult.Success -> {
                if (result.response.statusCode !in 200..299) {
                    mutableState.update {
                        it.copy(actionInFlight = false, failure = DynamicScreenFailure.Network("http_${result.response.statusCode}"))
                    }
                    return
                }
                when (action.responseMode) {
                    RequestResponseMode.NONE -> {
                        lastResult = result.response.body ?: JsonObject(emptyMap())
                        mutableState.update { it.copy(actionInFlight = false) }
                        snapshotCurrent()
                    }
                    RequestResponseMode.SCREEN -> {
                        val destination = action.destination ?: return failAction("screen_response_missing_destination")
                        val restorePolicy = if (action.method == RequestMethod.GET) DynamicRestorePolicy.CACHE_FIRST
                        else DynamicRestorePolicy.CACHE_ONLY
                        val next = DynamicDestination(
                            DynamicScreenInstruction(
                                destination = destination,
                                request = DynamicScreenRequest(
                                    action.method,
                                    action.endpoint,
                                    action.payload,
                                    action.authentication,
                                ),
                                transition = action.transition,
                                backStackKey = action.backStackKey?.takeIf { it.isNotBlank() } ?: destination.screenId,
                                restorePolicy = restorePolicy,
                            ),
                        )
                        consumeScreenResponse(next, destination, result.response.body)
                        if (state.value.failure == null) applyTransition(next, action.transition)
                    }
                }
            }
        }
    }

    private suspend fun executeCapability(action: PreparedAction.Capability) {
        when (val result = capabilityActions.execute(action)) {
            is CapabilityResult.Success -> {
                lastResult = result.payload
                mutableState.update { it.copy(actionInFlight = false) }
                snapshotCurrent()
            }
            CapabilityResult.Cancelled -> mutableState.update { it.copy(actionInFlight = false) }
            else -> failAction(result.toString())
        }
    }

    private fun executeNavigation(action: PreparedAction.Navigation) {
        snapshotCurrent()
        when (action.operation) {
            NavigationOperation.POP -> navigation.dispatch(NavigationCommand.Pop)
            NavigationOperation.POP_TO -> navigation.dispatch(
                NavigationCommand.PopTo(action.targetNavigationId ?: return failAction("pop_to_missing_target")),
            )
        }
        mutableState.update { it.copy(actionInFlight = false) }
    }

    private fun executePresentation(action: PreparedAction.Presentation) {
        val next = when (action.operation) {
            PresentationOperation.SHOW -> DynamicPresentationState(action.kind, action.id, action.properties)
            PresentationOperation.DISMISS -> null
        }
        mutableState.update { it.copy(actionInFlight = false, presentation = next) }
        snapshotCurrent()
    }

    private fun executeForm(action: PreparedAction.Form) {
        val form = currentForm ?: return failAction("form_store_unavailable")
        when (action.operation) {
            FormOperation.VALIDATE -> form.validate()
            FormOperation.RESET -> form.reset()
        }
        mutableState.update { it.copy(actionInFlight = false, form = form) }
        snapshotCurrent()
    }

    private suspend fun executeBackground(action: PreparedAction.Background) {
        when (val result = backgroundActions.execute(action)) {
            BackgroundActionResult.Success -> {
                lastResult = JsonObject(mapOf("background" to JsonPrimitive("success")))
                mutableState.update { it.copy(actionInFlight = false) }
                snapshotCurrent()
            }
            is BackgroundActionResult.Unsupported -> failAction(result.reason)
            is BackgroundActionResult.Rejected -> failAction(result.reason)
        }
    }

    private fun consumeScreenRequest(destination: DynamicDestination, expected: ScreenDestination, result: NetworkResult) {
        when (result) {
            is NetworkResult.Failure -> mutableState.update {
                it.copy(loading = false, failure = DynamicScreenFailure.Network(result.error.toString()))
            }
            is NetworkResult.Success -> {
                if (result.response.statusCode !in 200..299) {
                    mutableState.update {
                        it.copy(loading = false, failure = DynamicScreenFailure.Network("http_${result.response.statusCode}"))
                    }
                } else consumeScreenResponse(destination, expected, result.response.body)
            }
        }
    }

    private fun consumeScreenResponse(destination: DynamicDestination, expected: ScreenDestination, body: JsonElement?) {
        if (body == null) return failProtocol("missing_response_body")
        when (val processed = runtime.pipeline.process(body.toString())) {
            is SduiPipelineResult.Success -> acceptScreen(destination, expected, processed.screen)
            else -> failProtocol(processed.toString())
        }
    }

    private fun acceptScreen(destination: DynamicDestination, expected: ScreenDestination, screen: Screen) {
        if (
            screen.id.value != expected.screenId ||
            screen.template.id.value != expected.templateId ||
            screen.template.type != expected.templateType
        ) return failProtocol("destination_identity_mismatch")

        currentForm = formRuntime.create(screen)
        runtimeValues = emptyMap()
        lastResult = null
        lastExternalEvent = null
        mutableState.value = DynamicScreenState(destination = destination, screen = screen, form = currentForm)
        if (destination.instruction.restorePolicy != DynamicRestorePolicy.NETWORK_ONLY) snapshotCurrent()
    }

    private fun restore(destination: DynamicDestination, snapshot: DynamicScreenSnapshot) {
        currentForm = formRuntime.create(snapshot.screen)
        snapshot.formState?.let { currentForm?.restore(it) }
        runtimeValues = snapshot.runtimeValues
        lastResult = snapshot.lastResult
        lastExternalEvent = snapshot.lastExternalEvent
        mutableState.value = DynamicScreenState(
            destination = destination,
            screen = snapshot.screen,
            form = currentForm,
            presentation = snapshot.presentation,
        )
    }

    private fun snapshotCurrent() {
        val destination = state.value.destination ?: return
        val screen = state.value.screen ?: return
        if (destination.instruction.restorePolicy == DynamicRestorePolicy.NETWORK_ONLY) return
        cache.put(
            destination,
            DynamicScreenSnapshot(
                screen = screen,
                formState = currentForm?.state?.value,
                runtimeValues = runtimeValues,
                lastResult = lastResult,
                lastExternalEvent = lastExternalEvent,
                presentation = state.value.presentation,
            ),
        )
    }

    private fun updateFormFromEvent(event: SduiRenderEvent) {
        val changed = event as? SduiRenderEvent.ValueChanged ?: return
        val fieldId = changed.fieldId ?: changed.path.segments.lastOrNull()?.value ?: return
        currentForm?.update(FormFieldId(fieldId), JsonPrimitive(changed.value))
        snapshotCurrent()
    }

    private fun applyTransition(destination: DynamicDestination, transition: ScreenTransition) {
        when (transition) {
            ScreenTransition.PUSH -> navigation.dispatch(NavigationCommand.Push(destination))
            ScreenTransition.REPLACE -> navigation.dispatch(NavigationCommand.ReplaceTop(destination))
            ScreenTransition.RESET -> navigation.dispatch(NavigationCommand.ResetTo(destination))
            ScreenTransition.STAY -> if (navigation.state.value.current.navigationId != destination.navigationId) {
                navigation.dispatch(NavigationCommand.ReplaceTop(destination))
            }
        }
    }

    private fun failProtocol(detail: String) {
        mutableState.update {
            it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Protocol(detail))
        }
    }

    private fun failAction(detail: String) {
        mutableState.update { it.copy(actionInFlight = false, failure = DynamicScreenFailure.Action(detail)) }
    }

    fun close() {
        snapshotCurrent()
        loadJob?.cancel()
        actionJob?.cancel()
    }
}
