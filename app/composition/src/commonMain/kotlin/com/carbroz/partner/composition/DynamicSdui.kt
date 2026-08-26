package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkBody
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
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
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.protocol.SduiDecodeResult
import com.carbroz.runtime.sdui.protocol.SduiDecoder
import com.carbroz.runtime.sdui.protocol.SduiEnvelopeDto
import com.carbroz.runtime.sdui.protocol.SduiValidationResult
import com.carbroz.runtime.sdui.protocol.SduiValidator
import com.carbroz.runtime.sdui.normalization.SduiNormalizationResult
import com.carbroz.runtime.sdui.normalization.SduiNormalizer
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

/** Immutable canonical screen plus transient execution state. */
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

/** Screen runtime cache is intentionally distinct from raw HTTP response caching. */
class DynamicScreenCache {
    private val screens = mutableMapOf<String, Screen>()

    fun get(destination: DynamicDestination): Screen? = screens[destination.navigationId]
    fun put(destination: DynamicDestination, screen: Screen) { screens[destination.navigationId] = screen }
    fun remove(destination: DynamicDestination) { screens.remove(destination.navigationId) }
    fun clear() { screens.clear() }
}

/** Creates runtime binding values without exposing renderers to application services. */
fun interface DynamicBindingContextFactory {
    suspend fun create(screen: Screen?): BindingContext
}

/** Safe baseline binding context. Product/runtime sources can be added without changing the action engine. */
class DefaultDynamicBindingContextFactory : DynamicBindingContextFactory {
    override suspend fun create(screen: Screen?): BindingContext {
        val screenSource = BindingValueSource { path ->
            when (path.joinToString(".")) {
                "id" -> screen?.let { JsonPrimitive(it.id) }
                "template.id" -> screen?.let { JsonPrimitive(it.template.id) }
                "template.type" -> screen?.let { JsonPrimitive(it.template.type.value) }
                else -> null
            }
        }
        return BindingContext.of(BindingNamespace.SCREEN to screenSource)
    }
}

/**
 * Generic process-level owner of one active backend-driven SDUI destination.
 * It knows no business screen names and never performs networking from a renderer.
 */
class DynamicSduiStore(
    private val scope: CoroutineScope,
    private val network: NetworkDataSource,
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
            val request = destination.instruction.request
            val result = network.execute(request.toNetworkRequest())
            when (result) {
                is NetworkResult.Failure -> mutableState.update {
                    it.copy(loading = false, failure = DynamicScreenFailure.Network(result.error.toString()))
                }
                is NetworkResult.Success -> consumeScreenResponse(
                    destination = destination,
                    expected = destination.instruction.destination,
                    responseBody = result.response.body,
                    statusCode = result.response.statusCode,
                )
            }
        }
    }

    fun retry() {
        state.value.destination?.let { show(it, forceRefresh = true) }
    }

    fun onCommand(command: com.carbroz.runtime.sdui.model.Command) {
        if (state.value.actionInFlight) return
        actionJob?.cancel()
        actionJob = scope.launch {
            mutableState.update { it.copy(actionInFlight = true, failure = null) }
            val context = ActionPreparationContext(bindings = bindingContexts.create(state.value.screen), form = null)
            when (val prepared = actionPreparer.prepare(command, context)) {
                is ActionPreparationResult.Success -> execute(prepared.action)
                else -> mutableState.update {
                    it.copy(actionInFlight = false, failure = DynamicScreenFailure.Action(prepared.toString()))
                }
            }
        }
    }

    private suspend fun execute(action: PreparedAction) {
        try {
            when (action) {
                is PreparedAction.Request -> when (val result = networkActions.execute(action)) {
                    is NetworkResult.Failure -> mutableState.update {
                        it.copy(actionInFlight = false, failure = DynamicScreenFailure.Network(result.error.toString()))
                    }
                    is NetworkResult.Success -> {
                        val next = DynamicDestination(
                            DynamicScreenInstruction(
                                destination = action.destination,
                                request = DynamicScreenRequest(action.method, action.endpoint, action.payload),
                                transition = DynamicTransition.PUSH,
                            ),
                        )
                        consumeScreenResponse(next, action.destination, result.response.body, result.response.statusCode)
                        if (state.value.failure == null) applyTransition(next)
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
                it.copy(actionInFlight = false, failure = DynamicScreenFailure.Action(failure::class.simpleName ?: "action_failure"))
            }
        }
    }

    private fun consumeScreenResponse(
        destination: DynamicDestination,
        expected: ScreenDestination,
        responseBody: JsonElement?,
        statusCode: Int,
    ) {
        if (statusCode !in 200..299 || responseBody == null) {
            mutableState.update {
                it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Network("http_$statusCode"))
            }
            return
        }
        when (val decoded = runtime.decoder.decode(responseBody.toString())) {
            is SduiDecodeResult.Failure -> failProtocol(decoded.reason.name)
            is SduiDecodeResult.Success -> validateAndNormalize(destination, expected, decoded.envelope)
        }
    }

    private fun validateAndNormalize(
        destination: DynamicDestination,
        expected: ScreenDestination,
        envelope: SduiEnvelopeDto,
    ) {
        when (val validated = runtime.validator.validate(envelope)) {
            is SduiValidationResult.Failure -> failProtocol(validated.violations.joinToString { it.code.name })
            is SduiValidationResult.Success -> when (val normalized = runtime.normalizer.normalize(validated.envelope)) {
                is SduiNormalizationResult.Failure -> failProtocol(normalized.violations.joinToString { it.code.name })
                is SduiNormalizationResult.Success -> {
                    val screen = normalized.screen
                    if (screen.id != expected.screenId ||
                        screen.template.id != expected.templateId ||
                        screen.template.type != expected.templateType
                    ) {
                        failProtocol("destination_identity_mismatch")
                        return
                    }
                    cache.put(destination, screen)
                    mutableState.value = DynamicScreenState(destination = destination, screen = screen)
                }
            }
        }
    }

    private fun failProtocol(detail: String) {
        mutableState.update {
            it.copy(loading = false, actionInFlight = false, failure = DynamicScreenFailure.Protocol(detail))
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

private fun DynamicScreenRequest.toNetworkRequest(): NetworkRequest = NetworkRequest(
    method = when (method) {
        RequestMethod.GET -> NetworkMethod.GET
        RequestMethod.POST -> NetworkMethod.POST
        RequestMethod.PUT -> NetworkMethod.PUT
        RequestMethod.PATCH -> NetworkMethod.PATCH
        RequestMethod.DELETE -> NetworkMethod.DELETE
    },
    endpoint = NetworkEndpoint(endpoint),
    authentication = NetworkAuthentication.SESSION,
    cachePolicy = if (method == RequestMethod.GET) NetworkCachePolicy.NetworkFirst(60_000L) else NetworkCachePolicy.NetworkOnly,
    body = if (method == RequestMethod.GET) null else NetworkBody.Json(payload),
)
