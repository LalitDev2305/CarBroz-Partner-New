package com.carbroz.data.network

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ConfigurationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NetworkConfigurationTest {
    @Test
    fun canonicalConfigurationCreatesNetworkEnvironment() {
        val configuration = configuration(
            environment = AppEnvironment.Staging,
            apiBaseUrl = "https://staging-api.carbroz.test",
        )

        val environment = configuration.toNetworkEnvironment()

        assertEquals(configuration.apiBaseUrl, environment.baseUrl)
        assertEquals(AppEnvironment.Staging, environment.appEnvironment)
        assertEquals(
            "https://staging-api.carbroz.test/bootstrap",
            environment.resolve(NetworkEndpoint("/bootstrap")),
        )
    }

    @Test
    fun providerReadsCanonicalConfigurationWithoutDuplicatingEndpointSelection() {
        val configuration = configuration(
            environment = AppEnvironment.Production,
            apiBaseUrl = "https://api.carbroz.test",
        )
        val provider = NetworkEnvironmentProvider(ConfigurationProvider { configuration })

        assertEquals("https://api.carbroz.test", provider.get().baseUrl)
    }

    @Test
    fun localHttpIsAllowedOnlyForDevelopmentConfiguration() {
        val development = configuration(
            environment = AppEnvironment.Development,
            apiBaseUrl = "http://localhost:8080",
        )

        assertEquals("http://localhost:8080", development.toNetworkEnvironment().baseUrl)
        assertFailsWith<IllegalArgumentException> {
            NetworkEnvironment(
                baseUrl = "http://localhost:8080",
                appEnvironment = AppEnvironment.Production,
            )
        }
    }

    @Test
    fun invalidCanonicalConfigurationCannotInitializeNetworking() {
        val invalid = configuration(
            environment = AppEnvironment.Production,
            apiBaseUrl = "https://api.carbroz.test/",
        )

        assertFailsWith<IllegalArgumentException> {
            invalid.toNetworkEnvironment()
        }
    }

    private fun configuration(
        environment: AppEnvironment,
        apiBaseUrl: String,
    ) = AppConfiguration(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        buildInformation = BuildInformation(
            versionName = "1.0.0",
            versionCode = 1,
            applicationId = "com.carbroz.partner",
        ),
    )
}
