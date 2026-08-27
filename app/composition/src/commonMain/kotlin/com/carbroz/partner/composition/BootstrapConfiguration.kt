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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class BootstrapDestinationStore {
    private var resolved: DynamicScreenInstruction? = null
    fun resolve(instruction: DynamicScreenInstruction) { resolved = instruction }
    fun current(): DynamicScreenInstruction? = resolved
    fun clear() { resolved = null }
}

@Serializable
private data class BootstrapResponseDto(val nextScreen: kotlinx.serialization.json.JsonElement)

/** Session restore precedes bootstrap; optional auth lets the backend choose every first dynamic screen. */
class BootstrapConfigurationStartupTask(
    private val network: NetworkDataSource,
    private val destinations: BootstrapDestinationStore,
    private val instructionCodec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
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
                StartupFailure.Expected("bootstrap_http_${response.statusCode}", response.statusCode >= 500),
            )
        }
        val body = response.body ?: return invalidResponse("bootstrap_missing_body")
        val dto = runCatching { json.decodeFromJsonElement<BootstrapResponseDto>(body) }
            .getOrElse { return invalidResponse("bootstrap_invalid_payload") }
        return when (val decoded = instructionCodec.decode(dto.nextScreen)) {
            is DynamicInstructionDecodeResult.Success -> {
                destinations.resolve(decoded.instruction)
                StartupTaskResult.Success
            }
            is DynamicInstructionDecodeResult.Failure -> invalidResponse("bootstrap_${decoded.code}")
        }
    }

    private fun invalidResponse(code: String): StartupTaskResult.Failure =
        StartupTaskResult.Failure(StartupFailure.Expected(code, recoverable = false))

    private fun NetworkFailure.toStartupFailure(): StartupFailure.Expected = when (this) {
        NetworkFailure.Offline -> StartupFailure.Expected("bootstrap_offline", true)
        NetworkFailure.Timeout -> StartupFailure.Expected("bootstrap_timeout", true)
        is NetworkFailure.Http -> StartupFailure.Expected(
            "bootstrap_http_$statusCode",
            statusCode >= 500 || statusCode == 408 || statusCode == 429,
        )
        NetworkFailure.Transport -> StartupFailure.Expected("bootstrap_transport", true)
        is NetworkFailure.InvalidRequest -> StartupFailure.Expected("bootstrap_invalid_request", false)
    }
}
