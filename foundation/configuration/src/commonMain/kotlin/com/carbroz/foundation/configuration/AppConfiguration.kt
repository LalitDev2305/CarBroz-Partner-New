package com.carbroz.foundation.configuration

/**
 * Immutable product-neutral runtime configuration.
 *
 * This object carries only non-secret values safe to exist in the client binary.
 * Credentials and provider secrets must never be represented here.
 */
data class AppConfiguration(
    val environment: AppEnvironment,
    val apiBaseUrl: String,
    val buildInformation: BuildInformation,
)

/** Build identity supplied by the platform/application composition layer. */
data class BuildInformation(
    val versionName: String,
    val versionCode: Long,
    val applicationId: String,
)

/** Provides the immutable configuration selected for the current application process. */
fun interface ConfigurationProvider {
    fun get(): AppConfiguration
}
