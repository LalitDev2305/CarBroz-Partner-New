package com.carbroz.foundation.configuration

/**
 * Validates client-safe configuration before infrastructure is initialized.
 *
 * Validation intentionally checks structural invariants only. Network reachability,
 * certificates, authentication, and server compatibility belong to their respective
 * infrastructure/runtime layers.
 */
object ConfigurationValidator {
    fun validate(configuration: AppConfiguration): ConfigurationValidationResult {
        val violations = buildList {
            if (!configuration.apiBaseUrl.startsWith("https://")) {
                add(ConfigurationViolation.ApiBaseUrlMustUseHttps)
            }
            if (configuration.apiBaseUrl.endsWith('/')) {
                add(ConfigurationViolation.ApiBaseUrlMustNotEndWithSlash)
            }
            if (configuration.buildInformation.versionName.isBlank()) {
                add(ConfigurationViolation.VersionNameBlank)
            }
            if (configuration.buildInformation.versionCode <= 0L) {
                add(ConfigurationViolation.VersionCodeInvalid)
            }
            if (configuration.buildInformation.applicationId.isBlank()) {
                add(ConfigurationViolation.ApplicationIdBlank)
            }
        }
        return if (violations.isEmpty()) {
            ConfigurationValidationResult.Valid
        } else {
            ConfigurationValidationResult.Invalid(violations)
        }
    }
}

sealed interface ConfigurationValidationResult {
    data object Valid : ConfigurationValidationResult
    data class Invalid(val violations: List<ConfigurationViolation>) : ConfigurationValidationResult
}

enum class ConfigurationViolation {
    ApiBaseUrlMustUseHttps,
    ApiBaseUrlMustNotEndWithSlash,
    VersionNameBlank,
    VersionCodeInvalid,
    ApplicationIdBlank,
}
