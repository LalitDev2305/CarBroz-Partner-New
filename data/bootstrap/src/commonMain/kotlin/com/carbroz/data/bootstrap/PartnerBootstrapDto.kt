package com.carbroz.data.bootstrap

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class PartnerBootstrapEnvelopeDto(
    val success: Boolean,
    val message: String,
    val data: PartnerBootstrapDataDto? = null,
    val traceId: String? = null,
)

@Serializable
internal data class PartnerBootstrapDataDto(
    val config: PartnerBootstrapConfigDto,
    val startup: PartnerBootstrapStartupDto,
)

@Serializable
internal data class PartnerBootstrapConfigDto(
    val version: String,
    val maintenance: PartnerMaintenanceDto,
    val update: PartnerUpdateDto,
    val features: PartnerFeatureDto,
)

@Serializable
internal data class PartnerMaintenanceDto(
    val enabled: Boolean,
    val title: String? = null,
    val message: String? = null,
)

@Serializable
internal data class PartnerUpdateDto(
    val required: Boolean,
    val optional: Boolean,
    val minimumVersion: String,
    val latestVersion: String,
    val storeUrl: String? = null,
)

/** Decoded for wire compatibility; currently the backend remains authoritative for these choices. */
@Serializable
internal data class PartnerFeatureDto(
    val registrationEnabled: Boolean,
    val individualPartnerEnabled: Boolean,
    val organizationPartnerEnabled: Boolean,
)

@Serializable
internal data class PartnerBootstrapStartupDto(
    val authenticated: Boolean,
    val nextScreen: JsonElement,
)
