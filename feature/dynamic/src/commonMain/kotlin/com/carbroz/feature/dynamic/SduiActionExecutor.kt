package com.carbroz.feature.dynamic

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.foundation.capabilities.CapabilityKind
import com.carbroz.foundation.capabilities.CapabilityRegistry
import com.carbroz.foundation.capabilities.CapabilityResult
import com.carbroz.foundation.capabilities.GenericCapabilityRequest
import com.carbroz.foundation.security.Secret
import com.carbroz.foundation.session.AuthTokens
import com.carbroz.foundation.session.SessionState
import com.carbroz.foundation.session.SessionStore
import com.carbroz.foundation.session.SessionSubject
import com.carbroz.foundation.session.SessionTransitionResult
import com.carbroz.sdui.model.SduiAction
import com.carbroz.sdui.model.SduiAuthentication
import com.carbroz.sdui.model.SduiDestination
import com.carbroz.sdui.model.SduiNavigationMode
import com.carbroz.sdui.model.SduiRequestMethod
import com.carbroz.sdui.model.SduiRequestResponseMode
import com.carbroz.sdui.model.SduiStateOperation
import com.carbroz.sdui.model.SduiStateProperty
import com.carbroz.sdui.runtime.NodeRuntimeState
import com.carbroz.sdui.runtime.SduiOverlay
import com.carbroz.sdui.value.SduiExecutionContext
import com.carbroz.sdui.value.SduiValueResolution
import com.carbroz.sdui.value.SduiValueResolver
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

sealed interface SduiActionResult {
    data object Completed : SduiActionResult
    data object ValidationBlocked : SduiActionResult
    data class Navigate(
        val destination: DynamicDestination,
        val mode: SduiNavigationMode,
    ) : SduiActionResult

    data class NodeStateChanged(
        val targetId: String,
        val update: NodeRuntimeStateUpdate,
    ) : SduiActionResult

    data class OverlayChanged(
        val overlay: SduiOverlay?,
        val dismissTargetId: String? = null,
    ) : SduiActionResult

    data class Sequence(val results: List<SduiActionResult>) : SduiActionResult
    data class Failure(val reason: String) : SduiActionResult
}

data class NodeRuntimeStateUpdate(
    val operation: SduiStateOperation,
    val property: SduiStateProperty,
    val value: JsonElement?,
)

/** Executes only the seven wire actions. UI state reduction remains in DynamicScreenStore. */
class SduiActionExecutor(
    private val network: NetworkDataSource,
    private val capabilities: CapabilityRegistry,
    private val values: SduiValueResolver,
    private val contextProvider: DynamicContextProvider,
    private val flowContext: DynamicFlowContext,
    private val sessionStore: SessionStore,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
    },
) {
    suspend fun execute(
        action: SduiAction,
        bindings: Map<String, JsonElement>,
        validate: () -> Boolean = { true },
    ): SduiActionResult = when (action) {
        is SduiAction.Request -> {
            if (action.payload.validate && !validate()) SduiActionResult.ValidationBlocked
            else executeRequest(action, bindings)
        }
        is SduiAction.Navigate -> SduiActionResult.Navigate(
            destination = DynamicDestination.from(action.payload),
            mode = action.navigationMode,
        )
        is SduiAction.Present -> SduiActionResult.OverlayChanged(
            overlay = SduiOverlay(action.targetId, action.payload.presentation),
        )
        is SduiAction.Dismiss -> SduiActionResult.OverlayChanged(
            overlay = null,
            dismissTargetId = action.targetId,
        )
        is SduiAction.State -> SduiActionResult.NodeStateChanged(
            targetId = action.targetId,
            update = NodeRuntimeStateUpdate(
                operation = action.payload.operation,
                property = action.payload.property,
                value = action.payload.value,
            ),
        )
        is SduiAction.ExternalUri -> executeExternalUri(action, bindings)
        is SduiAction.Sequence -> executeSequence(action, bindings, validate)
    }

    private suspend fun executeRequest(
        action: SduiAction.Request,
        bindings: Map<String, JsonElement>,
    ): SduiActionResult {
        val execution = executionContext(bindings)
        val payload = action.payload.body?.let { body ->
            when (val resolved = values.resolve(body, execution)) {
                is SduiValueResolution.Success -> resolved.value as? JsonObject
                    ?: return SduiActionResult.Failure("request_body_not_object")
                is SduiValueResolution.Failure -> return SduiActionResult.Failure(resolved.reason)
            }
        }
        val pendingContext = action.payload.contextUpdates?.let { update ->
            when (val resolved = values.resolve(update, execution)) {
                is SduiValueResolution.Success -> resolved.value as? JsonObject
                    ?: return SduiActionResult.Failure("context_updates_not_object")
                is SduiValueResolution.Failure -> return SduiActionResult.Failure(resolved.reason)
            }
        }

        val request = runCatching {
            NetworkRequest(
                method = action.payload.method.toNetworkMethod(),
                endpoint = NetworkEndpoint(action.payload.endpoint),
                payload = payload,
                authentication = action.payload.authentication.toNetworkAuthentication(),
            )
        }.getOrElse { return SduiActionResult.Failure("invalid_request") }

        return when (val result = network.execute(request)) {
            is NetworkResult.Failure -> SduiActionResult.Failure("network:${result.error}")
            is NetworkResult.Success -> {
                if (result.response.statusCode !in 200..299) {
                    return SduiActionResult.Failure("http_${result.response.statusCode}")
                }

                when (action.payload.responseMode) {
                    SduiRequestResponseMode.NONE -> {
                        flowContext.commitSuccessfulRequest(result.response.body, pendingContext)
                        SduiActionResult.Completed
                    }
                    SduiRequestResponseMode.DESTINATION -> {
                        val responseBody = result.response.body
                            ?: return SduiActionResult.Failure("missing_response_body")
                        val destination = extractNextDestination(responseBody)
                            ?: return SduiActionResult.Failure("destination_missing")
                        val dynamicDestination = runCatching { DynamicDestination.from(destination) }
                            .getOrElse { return SduiActionResult.Failure("destination_invalid") }
                        val sessionResult = ensureSessionFor(destination, responseBody)
                        if (sessionResult != null) return sessionResult

                        flowContext.commitSuccessfulRequest(responseBody, pendingContext)
                        SduiActionResult.Navigate(
                            destination = dynamicDestination,
                            mode = action.payload.navigationMode,
                        )
                    }
                }
            }
        }
    }

    private suspend fun executeExternalUri(
        action: SduiAction.ExternalUri,
        bindings: Map<String, JsonElement>,
    ): SduiActionResult {
        val execution = executionContext(bindings)
        val uri = when (val resolved = values.resolve(action.payload.uri, execution)) {
            is SduiValueResolution.Success -> (resolved.value as? JsonPrimitive)?.content
                ?: return SduiActionResult.Failure("external_uri_not_string")
            is SduiValueResolution.Failure -> return SduiActionResult.Failure(resolved.reason)
        }
        if (!(uri.startsWith("https://") || uri.startsWith("http://") || uri.startsWith("mailto:") || uri.startsWith("tel:"))) {
            return SduiActionResult.Failure("external_uri_unsafe")
        }
        return when (
            val result = capabilities.execute(
                GenericCapabilityRequest(
                    kind = CapabilityKind.EXTERNAL_URI,
                    operation = "open",
                    arguments = mapOf("uri" to JsonPrimitive(uri)),
                ),
            )
        ) {
            is CapabilityResult.Success -> SduiActionResult.Completed
            CapabilityResult.Cancelled -> SduiActionResult.Completed
            is CapabilityResult.Failure -> SduiActionResult.Failure(result.code)
            is CapabilityResult.PermissionRequired -> SduiActionResult.Failure("capability_permission_required")
            is CapabilityResult.Restricted -> SduiActionResult.Failure("capability_restricted")
            is CapabilityResult.Unavailable -> SduiActionResult.Failure("capability_unavailable")
            is CapabilityResult.Unsupported -> SduiActionResult.Failure("capability_unsupported")
        }
    }

    private suspend fun executeSequence(
        action: SduiAction.Sequence,
        bindings: Map<String, JsonElement>,
        validate: () -> Boolean,
    ): SduiActionResult {
        val results = ArrayList<SduiActionResult>(action.payload.actions.size)
        for (child in action.payload.actions) {
            val result = execute(child, bindings, validate)
            if (result is SduiActionResult.Failure || result is SduiActionResult.ValidationBlocked) return result
            results += result
            if (result is SduiActionResult.Navigate) break
        }
        return SduiActionResult.Sequence(results)
    }

    private suspend fun executionContext(bindings: Map<String, JsonElement>): SduiExecutionContext {
        val flow = flowContext.snapshot()
        return SduiExecutionContext(
            bindings = bindings,
            context = contextProvider.current(),
            response = flow.response,
        )
    }

    private fun extractNextDestination(response: JsonElement): SduiDestination? {
        val root = response as? JsonObject ?: return null
        val data = root["data"] as? JsonObject ?: return null
        val next = data["nextScreen"] ?: return null
        return runCatching { json.decodeFromJsonElement<SduiDestination>(next) }.getOrNull()
    }

    /**
     * Auth APIs may establish a session and immediately return a SESSION destination. This applies
     * the canonical token response before navigation; ordinary authenticated business responses
     * simply reuse the already-current session.
     */
    private suspend fun ensureSessionFor(
        destination: SduiDestination,
        response: JsonElement,
    ): SduiActionResult.Failure? {
        if (destination.authentication != SduiAuthentication.SESSION) return null

        val root = response as? JsonObject ?: return SduiActionResult.Failure("session_response_invalid")
        val data = root["data"] as? JsonObject ?: return SduiActionResult.Failure("session_response_data_missing")
        val token = (data["token"] as? JsonPrimitive)?.content
        val refreshToken = (data["refreshToken"] as? JsonPrimitive)?.content
        val user = data["user"] as? JsonObject
        val subject = (user?.get("id") as? JsonPrimitive)?.content

        if (!token.isNullOrBlank() && !subject.isNullOrBlank()) {
            return when (
                sessionStore.authenticate(
                    SessionState.Authenticated(
                        subject = SessionSubject(subject),
                        tokens = AuthTokens(
                            accessToken = Secret.of(token),
                            refreshToken = refreshToken?.takeIf(String::isNotBlank)?.let(Secret::of),
                            accessTokenExpiresAtEpochMilliseconds = null,
                        ),
                    ),
                )
            ) {
                is SessionTransitionResult.Success -> null
                is SessionTransitionResult.Failed -> SduiActionResult.Failure("session_persist_failed")
            }
        }

        return when (sessionStore.current()) {
            is SessionState.Authenticated -> null
            SessionState.SignedOut -> SduiActionResult.Failure("session_credentials_missing")
        }
    }

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
}
