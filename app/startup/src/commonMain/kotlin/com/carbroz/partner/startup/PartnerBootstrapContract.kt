package com.carbroz.partner.startup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class PartnerBootstrapEnvelope(
    val success: Boolean,
    val message: String,
    val data: PartnerBootstrapData? = null,
    val traceId: String? = null,
)

@Serializable
data class PartnerBootstrapData(
    val config: PartnerBootstrapConfig,
    val startup: PartnerBootstrapStartup,
)

@Serializable
data class PartnerBootstrapConfig(
    val version: String,
    val maintenance: PartnerMaintenanceConfig,
    val update: PartnerUpdateConfig,
    val features: PartnerFeatureConfig,
) {
    init { require(version.isNotBlank()) { "Partner bootstrap config version must not be blank." } }
}

@Serializable
data class PartnerMaintenanceConfig(
    val enabled: Boolean,
    val title: String? = null,
    val message: String? = null,
)

@Serializable
data class PartnerUpdateConfig(
    val required: Boolean,
    val optional: Boolean,
    val minimumVersion: String,
    val latestVersion: String,
    val storeUrl: String? = null,
) {
    init {
        require(minimumVersion.isNotBlank()) { "Minimum version must not be blank." }
        require(latestVersion.isNotBlank()) { "Latest version must not be blank." }
        require(!(required && optional)) { "An update cannot be required and optional simultaneously." }
    }
}

@Serializable
data class PartnerFeatureConfig(
    val registrationEnabled: Boolean,
    val individualPartnerEnabled: Boolean,
    val organizationPartnerEnabled: Boolean,
)

@Serializable
data class PartnerBootstrapStartup(
    val authenticated: Boolean,
    val nextScreen: JsonElement,
)

sealed interface PartnerBootstrapClientResult {
    data class Success(val data: PartnerBootstrapData) : PartnerBootstrapClientResult
    data class Failure(val reason: PartnerBootstrapClientFailure) : PartnerBootstrapClientResult
}

sealed interface PartnerBootstrapClientFailure {
    data object Offline : PartnerBootstrapClientFailure
    data object Timeout : PartnerBootstrapClientFailure
    data object Transport : PartnerBootstrapClientFailure
    data class Http(val statusCode: Int) : PartnerBootstrapClientFailure
    data class InvalidPayload(val code: String) : PartnerBootstrapClientFailure {
        init { require(code.isNotBlank()) }
    }
}
