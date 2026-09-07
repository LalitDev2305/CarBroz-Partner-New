package com.carbroz.partner.startup

import com.carbroz.data.network.NetworkHeaderProvider
import com.carbroz.foundation.configuration.ConfigurationProvider

/** Supplies trusted process metadata to the canonical network stack. */
class ClientMetadataHeaderProvider(
    private val configurationProvider: ConfigurationProvider,
) : NetworkHeaderProvider {
    override suspend fun headers(): Map<String, String> {
        val configuration = configurationProvider.get()
        return mapOf(
            HEADER_PLATFORM to configuration.clientPlatform.name,
            HEADER_APP_VERSION to configuration.buildInformation.versionName,
            HEADER_BUILD_NUMBER to configuration.buildInformation.versionCode.toString(),
        )
    }

    companion object {
        const val HEADER_PLATFORM = "X-CarBroz-Platform"
        const val HEADER_APP_VERSION = "X-CarBroz-App-Version"
        const val HEADER_BUILD_NUMBER = "X-CarBroz-Build-Number"
    }
}
