package com.carbroz.partner.composition

import com.carbroz.foundation.configuration.AppConfiguration
import com.carbroz.foundation.configuration.AppEnvironment
import com.carbroz.foundation.configuration.BuildInformation
import com.carbroz.foundation.configuration.ClientPlatform
import com.carbroz.foundation.configuration.ConfigurationLoadResult
import com.carbroz.foundation.configuration.ConfigurationLoader
import com.carbroz.foundation.configuration.ConfigurationProvider

/** Creates the validated immutable process configuration consumed by shared runtime infrastructure. */
fun createCarBrozAppConfiguration(
    environment: String,
    apiBaseUrl: String,
    clientPlatform: ClientPlatform,
    versionName: String,
    versionCode: Long,
    applicationId: String,
): AppConfiguration {
    val selectedEnvironment = when (environment.trim().lowercase()) {
        "development", "dev" -> AppEnvironment.Development
        "staging", "stage" -> AppEnvironment.Staging
        "production", "prod" -> AppEnvironment.Production
        else -> error("Unsupported CarBroz environment: '$environment'.")
    }

    val provider = ConfigurationProvider {
        AppConfiguration(
            environment = selectedEnvironment,
            apiBaseUrl = apiBaseUrl.trim(),
            clientPlatform = clientPlatform,
            buildInformation = BuildInformation(
                versionName = versionName.trim(),
                versionCode = versionCode,
                applicationId = applicationId.trim(),
            ),
        )
    }

    return when (val result = ConfigurationLoader(provider).load()) {
        is ConfigurationLoadResult.Loaded -> result.configuration
        is ConfigurationLoadResult.Rejected -> error(
            "Invalid CarBroz application configuration: ${result.violations.joinToString()}",
        )
    }
}
