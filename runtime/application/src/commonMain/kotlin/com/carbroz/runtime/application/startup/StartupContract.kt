package com.carbroz.runtime.application.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Transport-independent HTTP method supported by the bootstrap-selected startup destination. */
enum class StartupRequestMethod { GET }

/** Transport-independent authentication requirement for the bootstrap-selected startup destination. */
enum class StartupAuthentication { NONE, SESSION }

/** Trusted initial backend-driven destination selected by Partner bootstrap policy. */
data class StartupDestination(
    val screenId: String,
    val templateId: String,
    val templateType: String,
    val endpoint: String,
    val method: StartupRequestMethod,
    val authentication: StartupAuthentication,
) {
    init {
        require(screenId.isNotBlank()) { "Startup screenId must not be blank." }
        require(templateId.isNotBlank()) { "Startup templateId must not be blank." }
        require(templateType.isNotBlank()) { "Startup templateType must not be blank." }
        require(endpoint.startsWith('/')) { "Startup endpoint must be relative." }
        require(!endpoint.startsWith("//")) { "Startup endpoint must not be protocol-relative." }
        require("://" !in endpoint) { "Startup endpoint must not be absolute." }
    }
}

data class BootstrapMaintenance(
    val enabled: Boolean,
    val title: String? = null,
    val message: String? = null,
)

data class BootstrapUpdate(
    val required: Boolean,
    val optional: Boolean,
    val minimumVersion: String,
    val latestVersion: String,
    val updateUri: String? = null,
) {
    init {
        require(minimumVersion.isNotBlank()) { "Bootstrap minimum version must not be blank." }
        require(latestVersion.isNotBlank()) { "Bootstrap latest version must not be blank." }
        require(!(required && optional)) { "Bootstrap update cannot be required and optional simultaneously." }
    }
}

data class PartnerFeatures(
    val registrationEnabled: Boolean,
    val individualPartnerEnabled: Boolean,
    val organizationPartnerEnabled: Boolean,
)

data class PartnerConfig(
    val version: String,
    val features: PartnerFeatures,
    val update: BootstrapUpdate,
) {
    init { require(version.isNotBlank()) { "Partner config version must not be blank." } }
}

/** Validated bootstrap model with HTTP/serialization details removed. */
data class BootstrapSnapshot(
    val authenticated: Boolean,
    val maintenance: BootstrapMaintenance,
    val config: PartnerConfig,
    val nextScreen: StartupDestination,
)

fun interface BootstrapRepository {
    suspend fun load(): BootstrapRepositoryResult
}

sealed interface BootstrapRepositoryResult {
    data class Success(val snapshot: BootstrapSnapshot) : BootstrapRepositoryResult
    data class Failure(val reason: BootstrapRepositoryFailure) : BootstrapRepositoryResult
}

sealed interface BootstrapRepositoryFailure {
    data object Offline : BootstrapRepositoryFailure
    data object Timeout : BootstrapRepositoryFailure
    data object Transport : BootstrapRepositoryFailure
    data class Http(val statusCode: Int) : BootstrapRepositoryFailure
    data class InvalidPayload(val code: String) : BootstrapRepositoryFailure {
        init { require(code.isNotBlank()) { "Bootstrap failure code must not be blank." } }
    }
}

/** Process-scoped owner of the latest reusable Partner configuration returned by bootstrap. */
class PartnerConfigStore {
    private val mutableState = MutableStateFlow<PartnerConfig?>(null)
    val state: StateFlow<PartnerConfig?> = mutableState.asStateFlow()

    fun update(config: PartnerConfig) {
        mutableState.value = config
    }

    fun current(): PartnerConfig? = mutableState.value
}

sealed interface StartupResult {
    data class Ready(val destination: StartupDestination) : StartupResult

    data class RequiredUpdate(
        val title: String,
        val message: String,
        val updateUri: String,
    ) : StartupResult

    data class Maintenance(
        val title: String?,
        val message: String?,
        val retryable: Boolean = true,
    ) : StartupResult

    data class Failure(
        val code: String,
        val recoverable: Boolean,
    ) : StartupResult {
        init { require(code.isNotBlank()) { "Startup failure code must not be blank." } }
    }
}
