package com.carbroz.foundation.configuration

/**
 * Validates client-safe configuration before infrastructure is initialized.
 *
 * Validation checks structural and environment-safety invariants only. Network
 * reachability, certificates, authentication, and server compatibility belong
 * to their respective infrastructure/runtime layers.
 */
object ConfigurationValidator {
    fun validate(configuration: AppConfiguration): ConfigurationValidationResult {
        val violations = buildList {
            validateBaseUrl(configuration, this)
            validateBuildInformation(configuration.buildInformation, this)
        }
        return if (violations.isEmpty()) {
            ConfigurationValidationResult.Valid
        } else {
            ConfigurationValidationResult.Invalid(violations)
        }
    }

    private fun validateBaseUrl(
        configuration: AppConfiguration,
        violations: MutableList<ConfigurationViolation>,
    ) {
        val url = configuration.apiBaseUrl
        val https = url.startsWith("https://")
        val localHttp = configuration.environment == AppEnvironment.Development &&
            (url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1"))

        if (!https && !localHttp) {
            violations += ConfigurationViolation.ApiBaseUrlSchemeInvalid
        }
        if (url.endsWith('/')) {
            violations += ConfigurationViolation.ApiBaseUrlMustNotEndWithSlash
        }
        if (url.contains('#')) {
            violations += ConfigurationViolation.ApiBaseUrlMustNotContainFragment
        }
        if (url.contains('?')) {
            violations += ConfigurationViolation.ApiBaseUrlMustNotContainQuery
        }
    }

    private fun validateBuildInformation(
        buildInformation: BuildInformation,
        violations: MutableList<ConfigurationViolation>,
    ) {
        if (buildInformation.versionName.isBlank()) {
            violations += ConfigurationViolation.VersionNameBlank
        }
        if (buildInformation.versionCode <= 0L) {
            violations += ConfigurationViolation.VersionCodeInvalid
        }
        if (!buildInformation.applicationId.matches(APPLICATION_ID_PATTERN)) {
            violations += ConfigurationViolation.ApplicationIdInvalid
        }
    }

    private val APPLICATION_ID_PATTERN = Regex(
        "^[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*){2,}$",
    )
}

sealed interface ConfigurationValidationResult {
    data object Valid : ConfigurationValidationResult
    data class Invalid(val violations: List<ConfigurationViolation>) : ConfigurationValidationResult
}

enum class ConfigurationViolation {
    ApiBaseUrlSchemeInvalid,
    ApiBaseUrlMustNotEndWithSlash,
    ApiBaseUrlMustNotContainFragment,
    ApiBaseUrlMustNotContainQuery,
    VersionNameBlank,
    VersionCodeInvalid,
    ApplicationIdInvalid,
}
