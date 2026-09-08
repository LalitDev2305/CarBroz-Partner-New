package com.carbroz.data.network

import com.carbroz.foundation.configuration.ConfigurationProvider

/** Supplies trusted immutable process metadata to every canonical REST request. */
class ConfigurationNetworkHeaderProvider(
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
        const val HEADER_PLATFORM: String = "X-CarBroz-Platform"
        const val HEADER_APP_VERSION: String = "X-CarBroz-App-Version"
        const val HEADER_BUILD_NUMBER: String = "X-CarBroz-Build-Number"
    }
}
