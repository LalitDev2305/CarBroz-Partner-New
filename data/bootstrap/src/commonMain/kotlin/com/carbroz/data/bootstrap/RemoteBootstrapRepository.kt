package com.carbroz.data.bootstrap

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkDecodeFailure
import com.carbroz.data.network.NetworkDecodedResult
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.executeTyped
import com.carbroz.runtime.application.startup.BootstrapMaintenance
import com.carbroz.runtime.application.startup.BootstrapRepository
import com.carbroz.runtime.application.startup.BootstrapRepositoryFailure
import com.carbroz.runtime.application.startup.BootstrapRepositoryResult
import com.carbroz.runtime.application.startup.BootstrapSnapshot
import com.carbroz.runtime.application.startup.BootstrapUpdate
import com.carbroz.runtime.application.startup.PartnerConfig
import com.carbroz.runtime.application.startup.PartnerFeatures
import com.carbroz.runtime.application.startup.StartupAuthentication
import com.carbroz.runtime.application.startup.StartupDestination
import com.carbroz.runtime.application.startup.StartupRequestMethod

/** Network-authoritative Partner bootstrap transport adapter. */
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
        if (status != 200 || code != "SUCCESS") return invalid("bootstrap_unsuccessful_envelope")
        val bootstrap = data ?: return invalid("bootstrap_missing_data")
        val config = bootstrap.config
        if (config.version.isBlank()) return invalid("bootstrap_invalid_config_version")

        val updateDto = config.update
        if (updateDto.minimumVersion.isBlank()) return invalid("bootstrap_invalid_minimum_version")
        if (updateDto.latestVersion.isBlank()) return invalid("bootstrap_invalid_latest_version")
        if (updateDto.required && updateDto.optional) return invalid("bootstrap_conflicting_update_policy")

        val destination = bootstrap.startup.nextScreen.toStartupDestination()
            ?: return invalid("bootstrap_invalid_next_screen")

        val update = BootstrapUpdate(
            required = updateDto.required,
            optional = updateDto.optional,
            minimumVersion = updateDto.minimumVersion,
            latestVersion = updateDto.latestVersion,
            updateUri = updateDto.storeUrl,
        )

        return BootstrapRepositoryResult.Success(
            BootstrapSnapshot(
                authenticated = bootstrap.startup.authenticated,
                maintenance = BootstrapMaintenance(
                    enabled = config.maintenance.enabled,
                    title = config.maintenance.title,
                    message = config.maintenance.message,
                ),
                config = PartnerConfig(
                    version = config.version,
                    features = PartnerFeatures(
                        registrationEnabled = config.features.registrationEnabled,
                        individualPartnerEnabled = config.features.individualPartnerEnabled,
                        organizationPartnerEnabled = config.features.organizationPartnerEnabled,
                    ),
                    update = update,
                ),
                nextScreen = destination,
            ),
        )
    }

    private fun PartnerStartupScreenDto.toStartupDestination(): StartupDestination? {
        val requestMethod = when (method) {
            "GET" -> StartupRequestMethod.GET
            else -> return null
        }
        val auth = when (authentication) {
            "NONE" -> StartupAuthentication.NONE
            "SESSION" -> StartupAuthentication.SESSION
            else -> return null
        }
        return runCatching {
            StartupDestination(
                screenId = screenId,
                templateId = templateId,
                templateType = templateType,
                endpoint = endpoint,
                method = requestMethod,
                authentication = auth,
            )
        }.getOrNull()
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
