package com.carbroz.feature.splash

import com.carbroz.feature.dynamic.DynamicScreenInstruction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** Client capabilities sent to bootstrap so the server can return a compatible startup contract. */
data class BootstrapClientCapabilities(
    val versionName: String,
    val versionCode: Long,
    val applicationId: String,
    val supportedBootstrapSchemaVersions: IntRange = 1..1,
    val supportedSduiProtocolVersions: IntRange = 1..1,
    val supportedSduiSchemaVersions: IntRange = 1..1,
) {
    init {
        require(versionName.isNotBlank())
        require(versionCode >= 0L)
        require(applicationId.isNotBlank())
        require(!supportedBootstrapSchemaVersions.isEmpty())
        require(!supportedSduiProtocolVersions.isEmpty())
        require(!supportedSduiSchemaVersions.isEmpty())
    }
}

@Serializable
enum class BootstrapUpdateMode {
    NONE,
    OPTIONAL,
    REQUIRED,
}

@Serializable
data class BootstrapMeta(
    val requestId: String? = null,
    val serverTimeEpochMilliseconds: Long,
    val bootstrapSchemaVersion: Int,
)

@Serializable
data class BootstrapRemoteConfiguration(
    val version: String,
    val data: JsonObject,
) {
    init { require(version.isNotBlank()) }
}

@Serializable
data class BootstrapUpdatePolicy(
    val mode: BootstrapUpdateMode = BootstrapUpdateMode.NONE,
    val title: String? = null,
    val message: String? = null,
    val storeUrl: String? = null,
    val minimumSupportedBuild: Long? = null,
    val latestBuild: Long? = null,
)

@Serializable
data class BootstrapMaintenancePolicy(
    val enabled: Boolean = false,
    val scope: String = "PARTNER_APP",
    val title: String? = null,
    val message: String? = null,
    val retryAfterSeconds: Long? = null,
    val supportAllowed: Boolean = true,
)

@Serializable
data class BootstrapSessionSnapshot(
    val authenticated: Boolean = false,
    val sessionId: String? = null,
    val expiresAtEpochMilliseconds: Long? = null,
)

@Serializable
data class BootstrapUserSnapshot(
    val id: String,
    val displayName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val status: String? = null,
)

@Serializable
data class BootstrapPartnerSnapshot(
    val partnerId: String,
    val partnerType: String? = null,
    val organizationId: String? = null,
    val onboardingStatus: String? = null,
    val verificationStatus: String? = null,
    val accountStatus: String? = null,
    val availabilityStatus: String? = null,
)

@Serializable
data class BootstrapSduiPolicy(
    val protocolVersion: Int,
    val schemaVersion: Int,
)

/**
 * Effective startup context for the current launch.
 *
 * Only [configuration] is persisted across launches. Session, identity, operational policies and
 * the destination are intentionally fresh server decisions and must never be restored from config cache.
 */
data class BootstrapSnapshot(
    val meta: BootstrapMeta,
    val configuration: BootstrapRemoteConfiguration,
    val updatePolicy: BootstrapUpdatePolicy,
    val maintenance: BootstrapMaintenancePolicy,
    val session: BootstrapSessionSnapshot,
    val user: BootstrapUserSnapshot?,
    val partner: BootstrapPartnerSnapshot?,
    val sdui: BootstrapSduiPolicy,
    val featureFlags: JsonObject,
    val capabilities: JsonObject,
    val serviceability: JsonObject,
    val realtime: JsonObject,
    val localization: JsonObject,
    val support: JsonObject,
    val runtimePolicy: JsonObject,
)

sealed interface BootstrapState {
    data object Idle : BootstrapState
    data object Fetching : BootstrapState

    data class Ready(
        val snapshot: BootstrapSnapshot,
        val instruction: DynamicScreenInstruction,
    ) : BootstrapState

    data class ForceUpdate(
        val snapshot: BootstrapSnapshot,
        val policy: BootstrapUpdatePolicy,
    ) : BootstrapState

    data class Maintenance(
        val snapshot: BootstrapSnapshot,
        val policy: BootstrapMaintenancePolicy,
    ) : BootstrapState
}

/** Single owner of the current launch's validated bootstrap outcome. */
class BootstrapStore {
    private val mutableState = MutableStateFlow<BootstrapState>(BootstrapState.Idle)
    val state: StateFlow<BootstrapState> = mutableState.asStateFlow()

    fun fetching() {
        mutableState.value = BootstrapState.Fetching
    }

    fun ready(snapshot: BootstrapSnapshot, instruction: DynamicScreenInstruction) {
        mutableState.value = BootstrapState.Ready(snapshot, instruction)
    }

    fun forceUpdate(snapshot: BootstrapSnapshot, policy: BootstrapUpdatePolicy) {
        mutableState.value = BootstrapState.ForceUpdate(snapshot, policy)
    }

    fun maintenance(snapshot: BootstrapSnapshot, policy: BootstrapMaintenancePolicy) {
        mutableState.value = BootstrapState.Maintenance(snapshot, policy)
    }

    fun currentInstruction(): DynamicScreenInstruction? =
        (mutableState.value as? BootstrapState.Ready)?.instruction

    fun reset() {
        mutableState.value = BootstrapState.Idle
    }
}
