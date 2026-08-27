package com.carbroz.partner.composition

import com.carbroz.data.network.NetworkAuthentication
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
import com.carbroz.runtime.sdui.model.RequestAuthentication
import com.carbroz.runtime.sdui.model.RequestMethod
import com.carbroz.runtime.sdui.model.ScreenDestination
import com.carbroz.runtime.sdui.model.ScreenTransition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/** Process-scoped owner of the validated first backend-driven screen instruction. */
class BootstrapDestinationStore {
    private var resolved: DynamicScreenInstruction? = null

    fun resolve(instruction: DynamicScreenInstruction) {
        resolved = instruction
    }

    fun current(): DynamicScreenInstruction? = resolved
    fun clear() { resolved = null }
}

@Serializable
private data class BootstrapResponseDto(val nextScreen: BootstrapScreenInstructionDto)

@Serializable
private data class BootstrapScreenInstructionDto(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: String = "GET",
    val authentication: String = "OPTIONAL_SESSION",
    val transition: String = "RESET",
    val backStackKey: String? = null,
    val restorePolicy: String = "CACHE_FIRST",
    val payload: JsonObject = JsonObject(emptyMap()),
)

/**
 * Session restoration runs before this task. Bootstrap then attaches that session only when one exists,
 * allowing the backend—not the client—to decide the first dynamic screen for both fresh and returning users.
 */
class BootstrapConfigurationStartupTask(
    private val network: NetworkDataSource,
    private val destinations: BootstrapDestinationStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : StartupTask {
    override val id: String = "bootstrap-configuration"

    override suspend fun execute(): StartupTaskResult = when (
        val result = network.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint("/api/v1/app"),
                authentication = NetworkAuthentication.OPTIONAL_SESSION,
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
        val body = response.body ?: return invalidResponse("bootstrap_missing_body")
        val dto = runCatching { json.decodeFromJsonElement<BootstrapResponseDto>(body) }
            .getOrElse { return invalidResponse("bootstrap_invalid_payload") }
        val instruction = dto.nextScreen.toInstruction()
            ?: return invalidResponse("bootstrap_invalid_next_screen")
        destinations.resolve(instruction)
        return StartupTaskResult.Success
    }

    private fun BootstrapScreenInstructionDto.toInstruction(): DynamicScreenInstruction? {
        if (screenId.isBlank() || templateId.isBlank() || templateType.isBlank()) return null
        if (!endpoint.startsWith('/') || endpoint.startsWith("//") || "://" in endpoint) return null
        val requestMethod = enumValue<RequestMethod>(method) ?: return null
        val requestAuthentication = enumValue<RequestAuthentication>(authentication) ?: return null
        val screenTransition = enumValue<ScreenTransition>(transition) ?: return null
        val policy = enumValue<DynamicRestorePolicy>(restorePolicy) ?: return null

        return DynamicScreenInstruction(
            destination = ScreenDestination(
                screenId = screenId,
                templateId = templateId,
                templateType = NodeType(templateType),
            ),
            request = DynamicScreenRequest(
                method = requestMethod,
                endpoint = endpoint,
                payload = payload,
                authentication = requestAuthentication,
            ),
            transition = screenTransition,
            backStackKey = backStackKey?.takeIf { it.isNotBlank() } ?: screenId,
            restorePolicy = policy,
        )
    }

    private inline fun <reified T : Enum<T>> enumValue(value: String): T? =
        enumValues<T>().firstOrNull { it.name == value.uppercase() }

    private fun invalidResponse(code: String): StartupTaskResult.Failure =
        StartupTaskResult.Failure(StartupFailure.Expected(code = code, recoverable = false))

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
}
