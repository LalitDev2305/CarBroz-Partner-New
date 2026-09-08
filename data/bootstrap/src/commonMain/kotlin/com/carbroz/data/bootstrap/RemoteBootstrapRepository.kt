package com.carbroz.data.bootstrap

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDecodeFailure
import com.carbroz.data.network.NetworkDecodedResult
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.executeTyped
import com.carbroz.runtime.application.bootstrap.BootstrapMaintenance
import com.carbroz.runtime.application.bootstrap.BootstrapRepository
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryFailure
import com.carbroz.runtime.application.bootstrap.BootstrapRepositoryResult
import com.carbroz.runtime.application.bootstrap.BootstrapSnapshot
import com.carbroz.runtime.application.bootstrap.BootstrapUpdate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToString

/**
 * Network-authoritative Partner bootstrap repository.
 *
 * HTTP mechanics, client metadata, auth recovery, retry and observability remain in data:network.
 * This adapter owns only the Partner route/DTO and maps it into the application bootstrap contract.
 */
class RemoteBootstrapRepository(
    private val network: NetworkDataSource,
) : BootstrapRepository {
    override suspend fun load(): BootstrapRepositoryResult {
        val request = NetworkRequest(
            method = NetworkMethod.GET,
            endpoint = BootstrapApiContract.endpoint,
            authentication = NetworkAuthentication.OPTIONAL_SESSION,
        )

        return when (
            val result = network.executeTyped(
                request = request,
                deserializer = PartnerBootstrapEnvelopeDto.serializer(),
            )
        ) {
            is NetworkDecodedResult.Failure -> BootstrapRepositoryResult.Failure(result.reason.toRepositoryFailure())
            is NetworkDecodedResult.Success -> result.value.toRepositoryResult()
        }
    }

    private fun PartnerBootstrapEnvelopeDto.toRepositoryResult(): BootstrapRepositoryResult {
        if (!success) return invalid("bootstrap_unsuccessful_envelope")
        val bootstrap = data ?: return invalid("bootstrap_missing_data")
        val config = bootstrap.config
        if (config.version.isBlank()) return invalid("bootstrap_invalid_config_version")

        val update = config.update
        if (update.minimumVersion.isBlank()) return invalid("bootstrap_invalid_minimum_version")
        if (update.latestVersion.isBlank()) return invalid("bootstrap_invalid_latest_version")
        if (update.required && update.optional) return invalid("bootstrap_conflicting_update_policy")

        val nextPayload = Json.encodeToString(
            JsonElement.serializer(),
            bootstrap.startup.nextScreen,
        )
        if (nextPayload.isBlank()) return invalid("bootstrap_missing_next_payload")

        return BootstrapRepositoryResult.Success(
            BootstrapSnapshot(
                authenticated = bootstrap.startup.authenticated,
                maintenance = BootstrapMaintenance(
                    enabled = config.maintenance.enabled,
                    title = config.maintenance.title,
                    message = config.maintenance.message,
                ),
                update = BootstrapUpdate(
                    required = update.required,
                    optional = update.optional,
                    minimumVersion = update.minimumVersion,
                    latestVersion = update.latestVersion,
                    updateUri = update.storeUrl,
                ),
                nextPayload = nextPayload,
            ),
        )
    }

    private fun NetworkDecodeFailure.toRepositoryFailure(): BootstrapRepositoryFailure = when (this) {
        is NetworkDecodeFailure.Network -> failure.toRepositoryFailure()
        NetworkDecodeFailure.MissingBody -> BootstrapRepositoryFailure.InvalidPayload("bootstrap_missing_body")
        NetworkDecodeFailure.InvalidBody -> BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_payload")
    }

    private fun NetworkFailure.toRepositoryFailure(): BootstrapRepositoryFailure = when (this) {
        NetworkFailure.Offline -> BootstrapRepositoryFailure.Offline
        NetworkFailure.Timeout -> BootstrapRepositoryFailure.Timeout
        NetworkFailure.Transport -> BootstrapRepositoryFailure.Transport
        is NetworkFailure.Http -> BootstrapRepositoryFailure.Http(statusCode)
        is NetworkFailure.InvalidRequest -> BootstrapRepositoryFailure.InvalidPayload("bootstrap_invalid_request")
    }

    private fun invalid(code: String): BootstrapRepositoryResult.Failure =
        BootstrapRepositoryResult.Failure(BootstrapRepositoryFailure.InvalidPayload(code))
}
