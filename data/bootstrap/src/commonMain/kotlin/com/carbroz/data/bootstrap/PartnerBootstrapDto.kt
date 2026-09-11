package com.carbroz.data.bootstrap

import kotlinx.serialization.Serializable

@Serializable
internal data class PartnerBootstrapEnvelopeDto(
    val status: Int,
    val code: String,
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

@Serializable
internal data class PartnerFeatureDto(
    val registrationEnabled: Boolean,
    val individualPartnerEnabled: Boolean,
    val organizationPartnerEnabled: Boolean,
)

@Serializable
internal data class PartnerBootstrapStartupDto(
    val authenticated: Boolean,
    val nextScreen: PartnerStartupScreenDto,
)

@Serializable
internal data class PartnerStartupScreenDto(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: String,
    val authentication: String,
)
