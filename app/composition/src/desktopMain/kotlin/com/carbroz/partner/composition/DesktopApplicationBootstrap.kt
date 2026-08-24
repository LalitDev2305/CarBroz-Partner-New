package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.DesktopCarBrozDatabaseBuilderProvider
import com.carbroz.data.securestorage.EphemeralDesktopSecureStorage
import java.io.File

/** Desktop host bridge using frozen credential policy and application-owned database storage. */
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
        databaseProvider = CarBrozDatabaseFactory(
            builderProvider = DesktopCarBrozDatabaseBuilderProvider(
                databaseDirectory = File(System.getProperty("user.home"), ".carbroz/$applicationId/database"),
            ),
        ),
    )
}
