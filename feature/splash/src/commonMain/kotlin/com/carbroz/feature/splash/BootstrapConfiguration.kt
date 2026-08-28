package com.carbroz.feature.splash

import com.carbroz.data.network.NetworkAuthentication
import com.carbroz.data.network.NetworkDataSource
import com.carbroz.data.network.NetworkEndpoint
import com.carbroz.data.network.NetworkFailure
import com.carbroz.data.network.NetworkMethod
import com.carbroz.data.network.NetworkRequest
import com.carbroz.data.network.NetworkResult
import com.carbroz.feature.dynamic.DynamicInstructionDecodeResult
import com.carbroz.feature.dynamic.DynamicScreenInstructionCodec
import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupTask
import com.carbroz.runtime.application.startup.StartupTaskResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Static-to-dynamic startup handoff.
 *
 * This task always asks the server for fresh startup context. Only the versioned global configuration
 * may be reused from local storage; user/partner/session/policies/nextScreen are never restored from cache.
 */
class BootstrapConfigurationStartupTask(
    private val network: NetworkDataSource,
    private val store: BootstrapStore,
    private val configurationCache: BootstrapConfigurationCache,
    private val client: BootstrapClientCapabilities,
    private val instructionCodec: DynamicScreenInstructionCodec = DynamicScreenInstructionCodec(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : StartupTask {
    override val id: String = "bootstrap-configuration"

    override suspend fun execute(): StartupTaskResult {
        store.fetching()
        val cachedConfiguration = readCachedConfiguration()
        val headers = buildMap {
            put(HEADER_APP_VERSION, client.versionName)
            put(HEADER_BUILD_NUMBER, client.versionCode.toString())
            put(HEADER_APPLICATION_ID, client.applicationId)
            put(HEADER_BOOTSTRAP_SCHEMA, client.supportedBootstrapSchemaVersions.last.toString())
            put(HEADER_SDUI_PROTOCOL, client.supportedSduiProtocolVersions.last.toString())
            put(HEADER_SDUI_SCHEMA, client.supportedSduiSchemaVersions.last.toString())
            cachedConfiguration?.version?.let { put(HEADER_CONFIG_VERSION, it) }
        }

        return when (
            val result = network.execute(
                NetworkRequest(
                    method = NetworkMethod.GET,
                    endpoint = NetworkEndpoint("/api/v1/app"),
                    headers = headers,
                    authentication = NetworkAuthentication.OPTIONAL_SESSION,
                ),
            )
        ) {
            is NetworkResult.Success -> handleSuccess(result, cachedConfiguration)
            is NetworkResult.Failure -> StartupTaskResult.Failure(result.error.toStartupFailure())
        }
    }

    private suspend fun handleSuccess(
        result: NetworkResult.Success,
        cachedConfiguration: BootstrapRemoteConfiguration?,
    ): StartupTaskResult {
        val response = result.response
        if (response.statusCode !in 200..299) {
            return StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_http_${response.statusCode}", response.statusCode >= 500),
            )
        }

        val body = response.body ?: return invalidResponse("bootstrap_missing_body")
        val dto = runCatching { json.decodeFromJsonElement<BootstrapResponseDto>(body) }
            .getOrElse { return invalidResponse("bootstrap_invalid_payload") }

        if (dto.meta.bootstrapSchemaVersion !in client.supportedBootstrapSchemaVersions) {
            return invalidResponse("bootstrap_schema_unsupported")
        }
        if (dto.sdui.protocolVersion !in client.supportedSduiProtocolVersions) {
            return invalidResponse("bootstrap_sdui_protocol_unsupported")
        }
        if (dto.sdui.schemaVersion !in client.supportedSduiSchemaVersions) {
            return invalidResponse("bootstrap_sdui_schema_unsupported")
        }

        val effectiveConfiguration = when {
            dto.config.changed -> {
                val data = dto.config.data ?: return invalidResponse("bootstrap_config_missing_data")
                BootstrapRemoteConfiguration(dto.config.version, data).also { configuration ->
                    writeCachedConfiguration(configuration)
                }
            }
            cachedConfiguration?.version == dto.config.version -> cachedConfiguration
            else -> return invalidResponse("bootstrap_config_cache_miss")
        }

        val updateRequiredByBuild = dto.updatePolicy.minimumSupportedBuild
            ?.let { client.versionCode < it }
            ?: false
        val effectiveUpdatePolicy = if (updateRequiredByBuild && dto.updatePolicy.mode != BootstrapUpdateMode.REQUIRED) {
            dto.updatePolicy.copy(mode = BootstrapUpdateMode.REQUIRED)
        } else {
            dto.updatePolicy
        }

        val snapshot = BootstrapSnapshot(
            meta = dto.meta,
            configuration = effectiveConfiguration,
            updatePolicy = effectiveUpdatePolicy,
            maintenance = dto.maintenance,
            session = dto.session,
            user = dto.user,
            partner = dto.partner,
            sdui = dto.sdui,
            featureFlags = dto.featureFlags,
            capabilities = dto.capabilities,
            serviceability = dto.serviceability,
            realtime = dto.realtime,
            localization = dto.localization,
            support = dto.support,
            runtimePolicy = dto.runtimePolicy,
        )

        if (effectiveUpdatePolicy.mode == BootstrapUpdateMode.REQUIRED) {
            if (effectiveUpdatePolicy.storeUrl.isNullOrBlank()) {
                return invalidResponse("bootstrap_required_update_missing_store_url")
            }
            store.forceUpdate(snapshot, effectiveUpdatePolicy)
            return StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_update_required", recoverable = false),
            )
        }

        if (dto.maintenance.enabled) {
            store.maintenance(snapshot, dto.maintenance)
            return StartupTaskResult.Failure(
                StartupFailure.Expected("bootstrap_maintenance", recoverable = true),
            )
        }

        val nextScreen = dto.nextScreen ?: return invalidResponse("bootstrap_missing_next_screen")
        return when (val decoded = instructionCodec.decode(nextScreen)) {
            is DynamicInstructionDecodeResult.Success -> {
                store.ready(snapshot, decoded.instruction)
                StartupTaskResult.Success
            }
            is DynamicInstructionDecodeResult.Failure -> invalidResponse("bootstrap_${decoded.code}")
        }
    }

    /** Cache is a startup optimization. Storage defects never own application availability. */
    private suspend fun readCachedConfiguration(): BootstrapRemoteConfiguration? = try {
        configurationCache.read()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Throwable) {
        null
    }

    /** Fresh server configuration remains valid even when local persistence is temporarily unavailable. */
    private suspend fun writeCachedConfiguration(configuration: BootstrapRemoteConfiguration) {
        try {
            configurationCache.write(configuration)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Best-effort cache only. A later launch will simply request the complete configuration again.
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

    @Serializable
    private data class BootstrapResponseDto(
        val meta: BootstrapMeta,
        val config: ConfigurationEnvelope,
        val updatePolicy: BootstrapUpdatePolicy = BootstrapUpdatePolicy(),
        val maintenance: BootstrapMaintenancePolicy = BootstrapMaintenancePolicy(),
        val session: BootstrapSessionSnapshot = BootstrapSessionSnapshot(),
        val user: BootstrapUserSnapshot? = null,
        val partner: BootstrapPartnerSnapshot? = null,
        val sdui: BootstrapSduiPolicy,
        val featureFlags: JsonObject = JsonObject(emptyMap()),
        val capabilities: JsonObject = JsonObject(emptyMap()),
        val serviceability: JsonObject = JsonObject(emptyMap()),
        val realtime: JsonObject = JsonObject(emptyMap()),
        val localization: JsonObject = JsonObject(emptyMap()),
        val support: JsonObject = JsonObject(emptyMap()),
        val runtimePolicy: JsonObject = JsonObject(emptyMap()),
        val nextScreen: JsonElement? = null,
    )

    @Serializable
    private data class ConfigurationEnvelope(
        val changed: Boolean,
        val version: String,
        val data: JsonObject? = null,
    ) {
        init { require(version.isNotBlank()) }
    }

    private companion object {
        const val HEADER_APP_VERSION = "X-CarBroz-App-Version"
        const val HEADER_BUILD_NUMBER = "X-CarBroz-Build-Number"
        const val HEADER_APPLICATION_ID = "X-CarBroz-Application-Id"
        const val HEADER_BOOTSTRAP_SCHEMA = "X-CarBroz-Bootstrap-Schema"
        const val HEADER_SDUI_PROTOCOL = "X-CarBroz-Sdui-Protocol"
        const val HEADER_SDUI_SCHEMA = "X-CarBroz-Sdui-Schema"
        const val HEADER_CONFIG_VERSION = "X-CarBroz-Config-Version"
    }
}
