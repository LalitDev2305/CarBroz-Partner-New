package com.carbroz.foundation.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ConfigurationLoaderTest {
    @Test
    fun `valid provider configuration is exposed`() {
        val expected = validConfiguration()
        val result = ConfigurationLoader(ConfigurationProvider { expected }).load()

        assertEquals(expected, assertIs<ConfigurationLoadResult.Loaded>(result).configuration)
    }

    @Test
    fun `invalid provider configuration is rejected before use`() {
        val invalid = validConfiguration().copy(apiBaseUrl = "http://api.carbroz.com")
        val result = ConfigurationLoader(ConfigurationProvider { invalid }).load()

        assertEquals(
            listOf(ConfigurationViolation.ApiBaseUrlSchemeInvalid),
            assertIs<ConfigurationLoadResult.Rejected>(result).violations,
        )
    }

    private fun validConfiguration() = AppConfiguration(
        environment = AppEnvironment.Production,
        apiBaseUrl = "https://api.carbroz.com",
        buildInformation = BuildInformation(
            versionName = "1.0.0",
            versionCode = 1L,
            applicationId = "com.carbroz.partner",
        ),
    )
}
