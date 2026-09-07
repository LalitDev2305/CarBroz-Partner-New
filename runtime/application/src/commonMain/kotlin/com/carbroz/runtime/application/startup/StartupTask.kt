package com.carbroz.runtime.application.startup

/** One ordered unit of application bootstrap work. */
interface StartupTask {
    val id: String
    suspend fun execute(): StartupTaskResult
}

/** Marker for a trusted product-specific payload produced by the final startup resolver. */
interface StartupPayload

/** Result of executing one startup task. */
sealed interface StartupTaskResult {
    /** The task completed successfully and startup should continue to the next task. */
    data object Continue : StartupTaskResult

    /** The task resolved the final startup outcome; no later startup task is executed. */
    data class Resolved(
        val resolution: StartupResolution,
    ) : StartupTaskResult

    /** A genuine startup failure. Maintenance and required update are not failures. */
    data class Failure(
        val reason: StartupFailure,
    ) : StartupTaskResult
}

/** Final trusted startup outcome returned by the resolving startup task. */
sealed interface StartupResolution {
    data class Ready(
        val payload: StartupPayload,
        val notices: List<StartupNotice> = emptyList(),
    ) : StartupResolution

    data class Blocked(
        val blocker: StartupBlocker,
    ) : StartupResolution
}

/** Valid server/application policy that intentionally blocks startup. */
sealed interface StartupBlocker {
    val retryable: Boolean

    data class RequiredUpdate(
        val title: String?,
        val message: String?,
        val updateUri: String,
    ) : StartupBlocker {
        init { require(updateUri.isNotBlank()) { "Required update URI must not be blank." } }
        override val retryable: Boolean = false
    }

    data class Maintenance(
        val title: String?,
        val message: String?,
        override val retryable: Boolean = true,
    ) : StartupBlocker
}

/** Non-blocking startup policy information. */
sealed interface StartupNotice {
    data class OptionalUpdate(
        val latestVersion: String,
        val updateUri: String?,
    ) : StartupNotice {
        init { require(latestVersion.isNotBlank()) { "Optional-update version must not be blank." } }
    }
}

/** Stable runtime failure vocabulary exposed by startup. */
sealed interface StartupFailure {
    val recoverable: Boolean

    data class Expected(
        val code: String,
        override val recoverable: Boolean,
    ) : StartupFailure {
        init { require(code.isNotBlank()) { "Startup failure code must not be blank." } }
    }

    data object Unexpected : StartupFailure {
        override val recoverable: Boolean = false
    }
}
