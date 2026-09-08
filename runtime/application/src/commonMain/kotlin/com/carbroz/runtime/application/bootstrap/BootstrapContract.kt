package com.carbroz.runtime.application.bootstrap

import com.carbroz.runtime.application.startup.StartupFailure
import com.carbroz.runtime.application.startup.StartupPayload
import com.carbroz.runtime.application.startup.StartupResolution

/** Trusted application-level bootstrap model with all transport details removed. */
data class BootstrapSnapshot(
    val authenticated: Boolean,
    val maintenance: BootstrapMaintenance,
    val update: BootstrapUpdate,
    val nextPayload: String,
) {
    init {
        require(nextPayload.isNotBlank()) { "Bootstrap next payload must not be blank." }
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

/** Application-owned data boundary for obtaining the current server-authoritative bootstrap snapshot. */
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

/**
 * Inward application contract for turning an opaque server bootstrap payload into a trusted
 * startup payload. Product/dynamic feature code implements this contract without creating an
 * outward runtime dependency.
 */
fun interface StartupPayloadDecoder {
    fun decode(payload: String): StartupPayloadDecodeResult
}

sealed interface StartupPayloadDecodeResult {
    data class Success(val payload: StartupPayload) : StartupPayloadDecodeResult
    data class Failure(val code: String) : StartupPayloadDecodeResult {
        init { require(code.isNotBlank()) { "Startup payload decode failure code must not be blank." } }
    }
}

/** Transport-independent outcome of resolving current bootstrap policy. */
sealed interface ResolveBootstrapResult {
    data class Resolved(val resolution: StartupResolution) : ResolveBootstrapResult
    data class Failed(val failure: StartupFailure) : ResolveBootstrapResult
}
