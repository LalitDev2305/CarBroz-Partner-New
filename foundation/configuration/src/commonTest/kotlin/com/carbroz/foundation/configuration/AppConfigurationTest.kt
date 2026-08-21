package com.carbroz.foundation.configuration

import kotlin.test.Test
import kotlin.test.assertEquals

class AppConfigurationTest {
    @Test
    fun `provider returns immutable typed configuration`() {
        val expected = AppConfiguration(
            environment = AppEnvironment.Staging,
            apiBaseUrl = "https://staging.example.invalid",
            buildInformation = BuildInformation(
                versionName = "1.0.0",
                versionCode = 1,
                applicationId = "com.carbroz.partner",
            ),
        )
        val provider = ConfigurationProvider { expected }

        assertEquals(expected, provider.get())
    }
}
