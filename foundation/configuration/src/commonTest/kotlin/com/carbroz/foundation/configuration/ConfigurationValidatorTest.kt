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
    fun `all structural violations are reported together`() {
        val result = ConfigurationValidator.validate(
            AppConfiguration(
                environment = AppEnvironment.Development,
                apiBaseUrl = "http://localhost/",
                buildInformation = BuildInformation(
                    versionName = " ",
                    versionCode = 0,
                    applicationId = "",
                ),
            ),
        )

        val invalid = assertIs<ConfigurationValidationResult.Invalid>(result)
        assertEquals(ConfigurationViolation.entries.toList(), invalid.violations)
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
