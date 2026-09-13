package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.navigation.NavigationCommand
import com.carbroz.foundation.navigation.NavigationStore
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiNavigationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiScreen
import com.carbroz.sdui.model.SduiStateOperation
import com.carbroz.sdui.model.SduiStateProperty
import com.carbroz.sdui.model.elementsInOrder
import com.carbroz.sdui.model.findElement
import com.carbroz.sdui.parser.SduiDecodeResult
import com.carbroz.sdui.parser.SduiDecoder
import com.carbroz.sdui.parser.SduiSupportChecker
import com.carbroz.sdui.parser.SduiSupportResult
import com.carbroz.sdui.render.SduiInteraction
import com.carbroz.sdui.runtime.FieldState
import com.carbroz.sdui.runtime.NodeRuntimeState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class DynamicScreenStore(
    private val scope: CoroutineScope,
    private val decoder: SduiDecoder,
    private val supportChecker: SduiSupportChecker,
    private val network: NetworkDataSource,
    private val actionExecutor: SduiActionExecutor,
    private val contextProvider: DynamicContextProvider,
    private val flowContext: DynamicFlowContext,
    private val navigation: NavigationStore,
) {
    private val mutableState = MutableStateFlow(DynamicScreenState())
    val state: StateFlow<DynamicScreenState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    fun dispatch(intent: DynamicScreenIntent) {
        when (intent) {
            is DynamicScreenIntent.Show -> show(intent.destination)
            DynamicScreenIntent.Retry -> state.value.destination?.let(::show)
            DynamicScreenIntent.Refresh -> state.value.destination?.let(::show)
            is DynamicScreenIntent.Interaction -> onInteraction(intent.value)
            DynamicScreenIntent.BackRequested -> navigation.dispatch(NavigationCommand.Pop)
        }
    }

    fun show(destination: DynamicDestination) {
        loadJob?.cancel()
        actionJob?.cancel()
        loadJob = scope.launch {
            mutableState.value = DynamicScreenState(destination = destination, loading = true)
            try {
                when (val result = network.execute(destination.toNetworkRequest())) {
                    is NetworkResult.Failure -> failNetwork(result.error.toString())
                    is NetworkResult.Success -> {
                        if (result.response.statusCode !in 200..299) {
                            failNetwork("http_${result.response.statusCode}")
                            return@launch
                        }
                        val body = result.response.body as? JsonObject
                            ?: return@launch failDecode("screen_envelope_missing")
                        val data = body["data"]
                            ?: return@launch failDecode("screen_data_missing")
                        when (val decoded = decoder.decode(data)) {
                            is SduiDecodeResult.Failure -> failDecode(decoded.reason)
                            is SduiDecodeResult.Success -> acceptScreen(destination, decoded.screen)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                failDecode(failure::class.simpleName ?: "screen_load_failure")
            }
        }
    }

    fun suspendForBackground() {
        loadJob?.cancel()
        actionJob?.cancel()
        loadJob = null
        actionJob = null
        mutableState.update { it.copy(loading = false, actionInFlight = false) }
    }

    fun close() {
        loadJob?.cancel()
        actionJob?.cancel()
    }

    private suspend fun acceptScreen(destination: DynamicDestination, screen: SduiScreen) {
        if (
            screen.screenId != destination.screenId ||
            screen.template.id != destination.templateId ||
            screen.template.type != destination.templateType
        ) {
            failUnsupported("destination_identity_mismatch")
            return
        }
        when (val support = supportChecker.check(screen)) {
            is SduiSupportResult.Unsupported -> {
                failUnsupported(support.reason)
                return
            }
            SduiSupportResult.Supported -> Unit
        }

        val flow = flowContext.snapshot()
        mutableState.value = DynamicScreenState(
            destination = destination,
            screen = screen,
            fields = initialFields(screen),
            context = contextProvider.current(),
            response = flow.response,
        )
    }

    private fun onInteraction(interaction: SduiInteraction) {
        when (interaction) {
            is SduiInteraction.ValueChanged -> {
                mutableState.update { current ->
                    val previous = current.fields[interaction.bindingKey] ?: FieldState()
                    current.copy(
                        fields = current.fields + (
                            interaction.bindingKey to previous.copy(
                                value = interaction.value,
                                error = null,
                                touched = true,
                            )
                        ),
                    )
                }
            }

            is SduiInteraction.ActionTriggered -> executeAction(interaction.action)
        }
    }

    private fun executeAction(action: SduiAction) {
        if (state.value.actionInFlight) return

        actionJob?.cancel()
        actionJob = scope.launch {
            mutableState.update { it.copy(actionInFlight = true, failure = null) }
            try {
                val bindings = state.value.fields.mapValues { (_, field) -> field.value }
                when (
                    val result = actionExecutor.execute(
                        action = action,
                        bindings = bindings,
                        validate = ::validateFields,
                    )
                ) {
                    is SduiActionResult.Failure -> failAction(result.reason)
                    else -> {
                        applyResult(result)
                        if (state.value.failure == null) {
                            val flow = flowContext.snapshot()
                            mutableState.update {
                                it.copy(
                                    actionInFlight = false,
                                    context = contextProvider.current(),
                                    response = flow.response,
                                )
                            }
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                failAction(failure::class.simpleName ?: "action_failure")
            }
        }
    }

    private suspend fun applyResult(result: SduiActionResult) {
        when (result) {
            SduiActionResult.Completed -> Unit
            SduiActionResult.ValidationBlocked -> Unit
            is SduiActionResult.Navigate -> applyNavigation(result.destination, result.mode)
            is SduiActionResult.NodeStateChanged -> applyNodeState(result)
            is SduiActionResult.OverlayChanged -> applyOverlayChange(result)
            is SduiActionResult.Sequence -> result.results.forEach { child -> applyResult(child) }
            is SduiActionResult.Failure -> failAction(result.reason)
        }
    }

    private fun applyOverlayChange(result: SduiActionResult.OverlayChanged) {
        mutableState.update { current ->
            when {
                result.overlay != null -> current.copy(overlay = result.overlay)
                result.dismissTargetId == null -> current.copy(overlay = null)
                current.overlay?.targetId == result.dismissTargetId -> current.copy(overlay = null)
                else -> current
            }
        }
    }

    private suspend fun applyNavigation(destination: DynamicDestination, mode: SduiNavigationMode) {
        when (mode) {
            SduiNavigationMode.PUSH -> navigation.dispatch(NavigationCommand.Push(destination))
            SduiNavigationMode.REPLACE -> navigation.dispatch(NavigationCommand.ReplaceTop(destination))
            SduiNavigationMode.RESET -> {
                flowContext.clear()
                navigation.dispatch(NavigationCommand.ResetTo(destination))
            }
        }
    }

    private fun applyNodeState(result: SduiActionResult.NodeStateChanged) {
        mutableState.update { current ->
            val update = result.update
            if (update.property == SduiStateProperty.VALUE) {
                val bindingKey = current.screen
                    ?.template
                    ?.findElement(result.targetId)
                    ?.binding
                    ?.key
                if (bindingKey != null) {
                    val previousField = current.fields[bindingKey] ?: FieldState()
                    return@update current.copy(
                        fields = current.fields + (
                            bindingKey to previousField.copy(
                                value = update.value ?: JsonNull,
                                error = null,
                            )
                        ),
                    )
                }
            }

            val previous = current.nodeStates[result.targetId] ?: NodeRuntimeState()
            val next = when (update.property) {
                SduiStateProperty.VISIBLE -> previous.copy(
                    visible = update.booleanValue(previous.visible ?: true),
                )
                SduiStateProperty.ENABLED -> previous.copy(
                    enabled = update.booleanValue(previous.enabled ?: true),
                )
                SduiStateProperty.SELECTED -> previous.copy(
                    selected = update.booleanValue(previous.selected ?: false),
                )
                SduiStateProperty.EXPANDED -> previous.copy(
                    expanded = update.booleanValue(previous.expanded ?: false),
                )
                SduiStateProperty.CHECKED -> previous.copy(
                    checked = update.booleanValue(previous.checked ?: false),
                )
                SduiStateProperty.LOADING -> previous.copy(
                    loading = update.booleanValue(previous.loading ?: false),
                )
                SduiStateProperty.VALUE -> previous.copy(value = update.value)
            }
            current.copy(nodeStates = current.nodeStates + (result.targetId to next))
        }
    }

    private fun NodeRuntimeStateUpdate.booleanValue(current: Boolean): Boolean = when (operation) {
        SduiStateOperation.TOGGLE -> !current
        SduiStateOperation.SET -> (value as? JsonPrimitive)?.takeIf { it.isString.not() }?.content?.toBooleanStrictOrNull()
            ?: (value as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
            ?: current
    }

    private fun validateFields(): Boolean {
        val screen = state.value.screen ?: return false
        var valid = true
        val updated = state.value.fields.toMutableMap()
        screen.template.elementsInOrder().forEach { element ->
            val key = element.binding?.key ?: return@forEach
            val rule = element.validation ?: return@forEach
            val field = updated[key] ?: FieldState()
            val text = (field.value as? JsonPrimitive)?.content.orEmpty()
            val pattern = rule.pattern
            val invalid = when {
                rule.required && (field.value is JsonNull || text.isBlank()) -> true
                pattern != null && text.isNotBlank() -> runCatching { !Regex(pattern).matches(text) }.getOrDefault(true)
                else -> false
            }
            updated[key] = field.copy(
                error = if (invalid) rule.message ?: "Invalid value" else null,
                touched = true,
            )
            if (invalid) valid = false
        }
        mutableState.update { it.copy(fields = updated) }
        return valid
    }

    private fun initialFields(screen: SduiScreen): Map<String, FieldState> = buildMap {
        screen.template.elementsInOrder().forEach { element ->
            val key = element.binding?.key ?: return@forEach
            put(key, FieldState(value = element.properties["value"] ?: JsonPrimitive("")))
        }
    }

    private fun DynamicDestination.toNetworkRequest(): NetworkRequest = NetworkRequest(
        method = method.toNetworkMethod(),
        endpoint = NetworkEndpoint(endpoint),
        authentication = authentication.toNetworkAuthentication(),
    )

    private fun SduiRequestMethod.toNetworkMethod(): NetworkMethod = when (this) {
        SduiRequestMethod.GET -> NetworkMethod.GET
        SduiRequestMethod.POST -> NetworkMethod.POST
        SduiRequestMethod.PUT -> NetworkMethod.PUT
        SduiRequestMethod.PATCH -> NetworkMethod.PATCH
        SduiRequestMethod.DELETE -> NetworkMethod.DELETE
    }

    private fun SduiAuthentication.toNetworkAuthentication(): NetworkAuthentication = when (this) {
        SduiAuthentication.NONE -> NetworkAuthentication.NONE
        SduiAuthentication.SESSION -> NetworkAuthentication.SESSION
    }

    private fun failNetwork(code: String) {
        mutableState.update { it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Network(code)) }
    }

    private fun failDecode(reason: String) {
        mutableState.update { it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Decode(reason)) }
    }

    private fun failUnsupported(reason: String) {
        mutableState.update {
            it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.UnsupportedContract(reason))
        }
    }

    private fun failAction(reason: String) {
        mutableState.update { it.copy(actionInFlight = false, failure = DynamicScreenFailure.Action(reason)) }
    }
}
