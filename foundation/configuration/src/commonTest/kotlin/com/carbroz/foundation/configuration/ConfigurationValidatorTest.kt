package com.carbroz.foundation.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConfigurationValidatorTest {
    @Test
    fun `valid production-safe configuration passes`() {
        assertEquals(ConfigurationValidationResult.Valid, ConfigurationValidator.validate(validConfiguration()))
    }

    @Test
    fun `development may use loopback http without weakening production`() {
        val development = validConfiguration().copy(
            environment = AppEnvironment.Development,
            apiBaseUrl = "http://localhost:8080",
        )
        val production = development.copy(environment = AppEnvironment.Production)

        assertEquals(ConfigurationValidationResult.Valid, ConfigurationValidator.validate(development))
        assertEquals(
            ConfigurationValidationResult.Invalid(listOf(ConfigurationViolation.ApiBaseUrlSchemeInvalid)),
            ConfigurationValidator.validate(production),
        )
    }

    @Test
    fun `unsafe structural violations are reported together`() {
        val result = ConfigurationValidator.validate(
            AppConfiguration(
                environment = AppEnvironment.Staging,
                apiBaseUrl = "http://api.example.com/path/?token=1#fragment/",
                buildInformation = BuildInformation(
                    versionName = " ",
                    versionCode = 0,
                    applicationId = "partner",
                ),
            ),
        )

        val invalid = assertIs<ConfigurationValidationResult.Invalid>(result)
        assertEquals(
            listOf(
                ConfigurationViolation.ApiBaseUrlSchemeInvalid,
                ConfigurationViolation.ApiBaseUrlMustNotEndWithSlash,
                ConfigurationViolation.ApiBaseUrlMustNotContainFragment,
                ConfigurationViolation.ApiBaseUrlMustNotContainQuery,
                ConfigurationViolation.VersionNameBlank,
                ConfigurationViolation.VersionCodeInvalid,
                ConfigurationViolation.ApplicationIdInvalid,
            ),
            invalid.violations,
        )
    }

    private fun validConfiguration() = AppConfiguration(
        environment = AppEnvironment.Production,
        apiBaseUrl = "https://api.carbroz.com",
        buildInformation = BuildInformation(
            versionName = "1.0.0",
            versionCode = 1,
            applicationId = "com.carbroz.partner",
        ),
    )
}
