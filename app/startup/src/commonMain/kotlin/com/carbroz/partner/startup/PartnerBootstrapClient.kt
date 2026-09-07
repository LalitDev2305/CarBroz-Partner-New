package com.carbroz.partner.startup

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

/** Executes and decodes the Partner bootstrap endpoint. Policy decisions live elsewhere. */
class PartnerBootstrapClient(
    private val network: NetworkDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun fetch(): PartnerBootstrapClientResult {
        val result = network.execute(
            NetworkRequest(
                method = NetworkMethod.GET,
                endpoint = NetworkEndpoint(ENDPOINT),
                authentication = NetworkAuthentication.OPTIONAL_SESSION,
            ),
        )

        return when (result) {
            is NetworkResult.Success -> decode(result)
            is NetworkResult.Failure -> PartnerBootstrapClientResult.Failure(result.error.toClientFailure())
        }
    }

    private fun decode(result: NetworkResult.Success): PartnerBootstrapClientResult {
        val body = result.response.body
            ?: return PartnerBootstrapClientResult.Failure(
                PartnerBootstrapClientFailure.InvalidPayload("bootstrap_missing_body"),
            )

        val envelope = try {
            json.decodeFromJsonElement<PartnerBootstrapEnvelope>(body)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            return PartnerBootstrapClientResult.Failure(
                PartnerBootstrapClientFailure.InvalidPayload("bootstrap_invalid_payload"),
            )
        }

        if (!envelope.success) {
            return PartnerBootstrapClientResult.Failure(
                PartnerBootstrapClientFailure.InvalidPayload("bootstrap_unsuccessful_envelope"),
            )
        }
        val data = envelope.data
            ?: return PartnerBootstrapClientResult.Failure(
                PartnerBootstrapClientFailure.InvalidPayload("bootstrap_missing_data"),
            )

        return PartnerBootstrapClientResult.Success(data)
    }

    private fun NetworkFailure.toClientFailure(): PartnerBootstrapClientFailure = when (this) {
        NetworkFailure.Offline -> PartnerBootstrapClientFailure.Offline
        NetworkFailure.Timeout -> PartnerBootstrapClientFailure.Timeout
        NetworkFailure.Transport -> PartnerBootstrapClientFailure.Transport
        is NetworkFailure.Http -> PartnerBootstrapClientFailure.Http(statusCode)
        is NetworkFailure.InvalidRequest ->
            PartnerBootstrapClientFailure.InvalidPayload("bootstrap_invalid_request")
    }

    companion object {
        const val ENDPOINT = "/api/v1/partner/bootstrap"
    }
}
