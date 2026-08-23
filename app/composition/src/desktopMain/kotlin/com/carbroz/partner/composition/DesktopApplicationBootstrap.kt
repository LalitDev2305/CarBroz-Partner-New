package com.carbroz.partner.composition

import com.carbroz.data.securestorage.EphemeralDesktopSecureStorage

/** Desktop host bridge using the frozen process-ephemeral credential policy. */
fun initializeCarBrozDesktopApplication(
    environment: String,
    apiBaseUrl: String,
    versionName: String,
    versionCode: Long,
    applicationId: String,
) {
    initializeCarBrozDependencyInjection(
        configuration = createCarBrozAppConfiguration(
            environment = environment,
            apiBaseUrl = apiBaseUrl,
            versionName = versionName,
            versionCode = versionCode,
            applicationId = applicationId,
        ),
        secureStorage = EphemeralDesktopSecureStorage(),
    )
}
