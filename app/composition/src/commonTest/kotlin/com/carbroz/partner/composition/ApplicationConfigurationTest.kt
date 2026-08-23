package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApplicationConfigurationTest {
    @Test
    fun mapsSupportedEnvironmentAliasesAndBuildMetadata() {
        val configuration = createCarBrozAppConfiguration(
            environment = "staging",
            apiBaseUrl = "https://staging.invalid",
            versionName = "1.2.3-staging",
            versionCode = 42L,
            applicationId = "com.carbroz.partner.staging",
        )

        assertEquals(AppEnvironment.Staging, configuration.environment)
        assertEquals("https://staging.invalid", configuration.apiBaseUrl)
        assertEquals(42L, configuration.buildInformation.versionCode)
    }

    @Test
    fun rejectsUnknownEnvironmentBeforeDiStarts() {
        assertFailsWith<IllegalStateException> {
            createCarBrozAppConfiguration(
                environment = "qa",
                apiBaseUrl = "https://qa.invalid",
                versionName = "1.0.0",
                versionCode = 1L,
                applicationId = "com.carbroz.partner.qa",
            )
        }
    }

    @Test
    fun rejectsUnsafeProductionBaseUrl() {
        assertFailsWith<IllegalStateException> {
            createCarBrozAppConfiguration(
                environment = "production",
                apiBaseUrl = "http://localhost:8080",
                versionName = "1.0.0",
                versionCode = 1L,
                applicationId = "com.carbroz.partner",
            )
        }
    }
}
