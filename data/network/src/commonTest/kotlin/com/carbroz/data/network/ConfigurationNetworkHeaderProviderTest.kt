package com.carbroz.data.network

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ClientPlatform
import com.carbroz.foundation.configuration.ConfigurationProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigurationNetworkHeaderProviderTest {
    @Test
    fun `provider emits exact platform version and build headers for every client platform`() = runTest {
        ClientPlatform.entries.forEach { platform ->
            val provider = ConfigurationNetworkHeaderProvider(
                ConfigurationProvider {
                    AppConfiguration(
                        environment = AppEnvironment.Development,
                        apiBaseUrl = "http://127.0.0.1:3000",
                        clientPlatform = platform,
                        buildInformation = BuildInformation(
                            versionName = "1.2.3-dev",
                            versionCode = 42L,
                            applicationId = "com.carbroz.partner.dev",
                        ),
                    )
                },
            )

            assertEquals(
                mapOf(
                    ConfigurationNetworkHeaderProvider.HEADER_PLATFORM to platform.name,
                    ConfigurationNetworkHeaderProvider.HEADER_APP_VERSION to "1.2.3-dev",
                    ConfigurationNetworkHeaderProvider.HEADER_BUILD_NUMBER to "42",
                ),
                provider.headers(),
            )
        }
    }
}
