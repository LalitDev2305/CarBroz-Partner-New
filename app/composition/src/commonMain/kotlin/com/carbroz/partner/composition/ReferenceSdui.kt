package com.carbroz.partner.composition

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.architecture.store.Store
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.runtime.action.ActionPreparationContext
import com.carbroz.runtime.action.ActionPreparationResult
import com.carbroz.runtime.action.ActionPreparer
import com.carbroz.runtime.action.PreparedAction
import com.carbroz.runtime.binding.BindingContext
import com.carbroz.runtime.sdui.SduiPipeline
import com.carbroz.runtime.sdui.SduiPipelineResult
import com.carbroz.runtime.sdui.model.Screen
import com.carbroz.runtime.sdui.rendering.DynamicScreenHost
import com.carbroz.runtime.sdui.rendering.SduiCommandIntent
import com.carbroz.runtime.sdui.rendering.SduiRenderFailure
import com.carbroz.runtime.sdui.rendering.SduiRendererDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** State for the product-neutral SDUI proof slice. */
data class ReferenceSduiState(
    val loading: Boolean = false,
    val actionInFlight: Boolean = false,
    val screen: Screen? = null,
    val failure: ReferenceSduiFailure? = null,
)

sealed interface ReferenceSduiIntent {
    data object Load : ReferenceSduiIntent
    data class Execute(val command: SduiCommandIntent) : ReferenceSduiIntent
    data class RenderFailed(val failure: SduiRenderFailure) : ReferenceSduiIntent
}

sealed interface ReferenceSduiFailure {
    data class Network(val reason: String) : ReferenceSduiFailure
    data class Pipeline(val reason: String) : ReferenceSduiFailure
    data class Action(val reason: String) : ReferenceSduiFailure
    data class Render(val failure: SduiRenderFailure) : ReferenceSduiFailure
}

/**
 * MVI owner for the neutral reference screen.
 *
 * Untrusted payloads always pass through [SduiPipeline]. Render events become semantic commands,
 * commands pass through [ActionPreparer] (including binding resolution), and only prepared actions
 * may cross into network/capability executors.
 */
class ReferenceSduiStore(
    private val network: NetworkDataSource,
    private val pipeline: SduiPipeline,
    private val actionPreparer: ActionPreparer,
    private val networkActions: NetworkActionExecutor,
    private val capabilityActions: CapabilityActionExecutor,
    parentScope: CoroutineScope,
) : Store<ReferenceSduiIntent, ReferenceSduiState> {
    private val parentJob = parentScope.coroutineContext[Job]
    private val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentJob))
    private val mutableState = MutableStateFlow(ReferenceSduiState())
    override val state: StateFlow<ReferenceSduiState> = mutableState.asStateFlow()

    private var loadJob: Job? = null
    private var actionJob: Job? = null

    override fun dispatch(intent: ReferenceSduiIntent) {
        when (intent) {
            ReferenceSduiIntent.Load -> load()
            is ReferenceSduiIntent.Execute -> execute(intent.command)
            is ReferenceSduiIntent.RenderFailed -> deferRenderFailure(intent.failure)
        }
    }

    fun close() {
        loadJob?.cancel()
        actionJob?.cancel()
        scope.cancel()
    }

    private fun load() {
        if (loadJob?.isActive == true || actionJob?.isActive == true) return
        loadJob = scope.launch {
            mutableState.value = mutableState.value.copy(loading = true, failure = null)
            val result = network.execute(
                NetworkRequest(
                    method = NetworkMethod.GET,
                    endpoint = NetworkEndpoint(REFERENCE_SCREEN_ENDPOINT),
                    authentication = NetworkAuthentication.SESSION,
                ),
            )
            mutableState.value = when (result) {
                is NetworkResult.Success -> consumeNetworkScreen(result, expected = null)
                is NetworkResult.Failure -> mutableState.value.copy(
                    loading = false,
                    failure = ReferenceSduiFailure.Network(result.error.toString()),
                )
            }
        }
    }

    private fun execute(intent: SduiCommandIntent) {
        if (actionJob?.isActive == true || loadJob?.isActive == true) return
        actionJob = scope.launch {
            mutableState.value = mutableState.value.copy(actionInFlight = true, failure = null)
            when (
                val preparation = actionPreparer.prepare(
                    command = intent.command,
                    context = ActionPreparationContext(bindings = BindingContext.of()),
                )
            ) {
                is ActionPreparationResult.Success -> executePrepared(preparation.action)
                else -> mutableState.value = mutableState.value.copy(
                    actionInFlight = false,
                    failure = ReferenceSduiFailure.Action(preparation.toString()),
                )
            }
        }
    }

    private fun deferRenderFailure(failure: SduiRenderFailure) {
        scope.launch {
            mutableState.value = mutableState.value.copy(
                screen = null,
                loading = false,
                actionInFlight = false,
                failure = ReferenceSduiFailure.Render(failure),
            )
        }
    }

    private suspend fun executePrepared(action: PreparedAction) {
        when (action) {
            is PreparedAction.Request -> {
                val result = networkActions.execute(action)
                mutableState.value = when (result) {
                    is NetworkResult.Success -> consumeNetworkScreen(result, expected = action)
                    is NetworkResult.Failure -> mutableState.value.copy(
                        actionInFlight = false,
                        failure = ReferenceSduiFailure.Network(result.error.toString()),
                    )
                }
            }

            is PreparedAction.Capability -> {
                mutableState.value = when (val result = capabilityActions.execute(action)) {
                    is CapabilityResult.Success,
                    CapabilityResult.Cancelled -> mutableState.value.copy(actionInFlight = false)

                    else -> mutableState.value.copy(
                        actionInFlight = false,
                        failure = ReferenceSduiFailure.Action(result.toString()),
                    )
                }
            }
        }
    }

    private fun consumeNetworkScreen(
        result: NetworkResult.Success,
        expected: PreparedAction.Request?,
    ): ReferenceSduiState {
        val response = result.response
        if (response.statusCode !in 200..299) {
            return mutableState.value.copy(
                loading = false,
                actionInFlight = false,
                failure = ReferenceSduiFailure.Network("http_${response.statusCode}"),
            )
        }
        val payload = response.body?.toString()
            ?: return mutableState.value.copy(
                loading = false,
                actionInFlight = false,
                failure = ReferenceSduiFailure.Pipeline("missing_response_body"),
            )

        return when (val processed = pipeline.process(payload)) {
            is SduiPipelineResult.Success -> {
                if (expected != null && !processed.screen.matches(expected)) {
                    mutableState.value.copy(
                        loading = false,
                        actionInFlight = false,
                        failure = ReferenceSduiFailure.Action("response_destination_mismatch"),
                    )
                } else {
                    ReferenceSduiState(screen = processed.screen)
                }
            }

            else -> mutableState.value.copy(
                loading = false,
                actionInFlight = false,
                failure = ReferenceSduiFailure.Pipeline(processed.toString()),
            )
        }
    }

    private fun Screen.matches(action: PreparedAction.Request): Boolean =
        id.value == action.destination.screenId &&
            template.id.value == action.destination.templateId &&
            template.type == action.destination.templateType

    private companion object {
        const val REFERENCE_SCREEN_ENDPOINT = "/api/v1/screen/reference"
    }
}

@Composable
fun ReferenceSduiScreen(
    state: ReferenceSduiState,
    dispatcher: SduiRendererDispatcher,
    onRetry: () -> Unit,
    onCommand: (SduiCommandIntent) -> Unit,
    onRenderFailure: (SduiRenderFailure) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            state.failure != null -> Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Unable to load this screen.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onRetry) { Text("Retry") }
            }

            state.loading && state.screen == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.screen != null -> DynamicScreenHost(
                screen = state.screen,
                dispatcher = dispatcher,
                onCommand = onCommand,
                onRenderFailure = onRenderFailure,
            )
        }

        if (state.actionInFlight) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
    }
}
