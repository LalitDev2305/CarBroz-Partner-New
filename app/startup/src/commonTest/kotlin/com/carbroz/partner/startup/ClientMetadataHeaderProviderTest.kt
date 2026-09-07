package com.carbroz.partner.startup

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ClientPlatform
import com.carbroz.foundation.configuration.ConfigurationProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientMetadataHeaderProviderTest {
    @Test
    fun `provider exposes platform version and build number from canonical configuration`() = runTest {
        val configuration = AppConfiguration(
            environment = AppEnvironment.Development,
            apiBaseUrl = "http://127.0.0.1:3000",
            clientPlatform = ClientPlatform.ANDROID,
            buildInformation = BuildInformation(
                versionName = "1.0.0",
                versionCode = 1L,
                applicationId = "com.carbroz.partner.dev",
            ),
        )
        val headers = ClientMetadataHeaderProvider(ConfigurationProvider { configuration }).headers()

        assertEquals("ANDROID", headers[ClientMetadataHeaderProvider.HEADER_PLATFORM])
        assertEquals("1.0.0", headers[ClientMetadataHeaderProvider.HEADER_APP_VERSION])
        assertEquals("1", headers[ClientMetadataHeaderProvider.HEADER_BUILD_NUMBER])
    }
}
