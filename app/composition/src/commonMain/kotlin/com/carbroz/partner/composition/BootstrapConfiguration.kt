package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkCachePolicy
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import com.carbroz.runtime.sdui.model.NodeType
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Process-scoped owner of the validated first backend-driven screen instruction. */
class BootstrapDestinationStore {
    private var resolved: DynamicScreenInstruction? = null

    fun resolve(instruction: DynamicScreenInstruction) {
        resolved = instruction
    }

    fun current(): DynamicScreenInstruction? = resolved
}

/**
 * Executes the application bootstrap/config request through the canonical network stack.
 * `/api/v1/app` is intentionally a public bootstrap endpoint; an already-restored session is
 * still owned by SessionStore and can be used by subsequent SESSION-authenticated screen calls.
 * The response is untrusted and is normalized into [DynamicScreenInstruction] before use.
 */
class BootstrapConfigurationStartupTask(
    private val network: NetworkDataSource,
    private val destinations: BootstrapDestinationStore,
) : StartupTask {
    override val id: String = "bootstrap-configuration"

    override suspend fun execute(): StartupTaskResult = when (
        val result = network.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/api/v1/app"),
                authentication = NetworkAuthentication.NONE,
                cachePolicy = NetworkCachePolicy.NetworkFirst(
                    fallbackMaxAgeMillis = BOOTSTRAP_CACHE_MAX_AGE_MILLIS,
                ),
            ),
        )
    ) {
        is NetworkResult.Success -> handleSuccess(result)
        is NetworkResult.Failure -> StartupTaskResult.Failure(result.error.toStartupFailure())
    }

    private fun handleSuccess(result: NetworkResult.Success): StartupTaskResult {
        val response = result.response
        if (response.statusCode !in 200..299) {
            return StartupTaskResult.Failure(
                StartupFailure.Expected(
                    code = "bootstrap_http_${response.statusCode}",
                    recoverable = response.statusCode >= 500,
                ),
            )
        }

        val body = response.body as? JsonObject ?: return invalidResponse("bootstrap_missing_object")
        val next = body["nextScreen"] as? JsonObject ?: return invalidResponse("bootstrap_missing_next_screen")
        val instruction = next.toInstruction() ?: return invalidResponse("bootstrap_invalid_next_screen")
        destinations.resolve(instruction)
        return StartupTaskResult.Success
    }

    private fun JsonObject.toInstruction(): DynamicScreenInstruction? {
        val screenId = string("screenId") ?: return null
        val templateId = string("templateId") ?: return null
        val templateType = string("templateType") ?: return null
        val endpoint = string("endpoint") ?: return null
        if (screenId.isBlank() || templateId.isBlank() || templateType.isBlank()) return null
        if (!endpoint.startsWith('/') || endpoint.startsWith("//")) return null

        val method = when (string("method")?.uppercase() ?: "GET") {
            "GET" -> RequestMethod.GET
            "POST" -> RequestMethod.POST
            "PUT" -> RequestMethod.PUT
            "PATCH" -> RequestMethod.PATCH
            "DELETE" -> RequestMethod.DELETE
            else -> return null
        }
        val transition = when (string("transition")?.uppercase() ?: "RESET") {
            "PUSH" -> DynamicTransition.PUSH
            "REPLACE" -> DynamicTransition.REPLACE
            "RESET" -> DynamicTransition.RESET
            "STAY" -> DynamicTransition.STAY
            else -> return null
        }
        val backStackKey = string("backStackKey")?.takeIf { it.isNotBlank() } ?: screenId
        val payload = this["payload"] as? JsonObject ?: JsonObject(emptyMap())

        return DynamicScreenInstruction(
            destination = ScreenDestination(
                screenId = screenId,
                templateId = templateId,
                templateType = NodeType(templateType),
            ),
            request = DynamicScreenRequest(method = method, endpoint = endpoint, payload = payload),
            transition = transition,
            backStackKey = backStackKey,
        )
    }

    private fun JsonObject.string(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull?.trim()

    private fun invalidResponse(code: String): StartupTaskResult.Failure =
        StartupTaskResult.Failure(
            StartupFailure.Expected(code = code, recoverable = false),
        )

    private fun NetworkFailure.toStartupFailure(): StartupFailure.Expected = when (this) {
        NetworkFailure.Offline -> StartupFailure.Expected("bootstrap_offline", recoverable = true)
        NetworkFailure.Timeout -> StartupFailure.Expected("bootstrap_timeout", recoverable = true)
        is NetworkFailure.Http -> StartupFailure.Expected(
            code = "bootstrap_http_$statusCode",
            recoverable = statusCode >= 500 || statusCode == 408 || statusCode == 429,
        )
        NetworkFailure.Transport -> StartupFailure.Expected("bootstrap_transport", recoverable = true)
        is NetworkFailure.InvalidRequest -> StartupFailure.Expected("bootstrap_invalid_request", recoverable = false)
    }

    private companion object {
        const val BOOTSTRAP_CACHE_MAX_AGE_MILLIS: Long = 5 * 60 * 1000L
    }
}
