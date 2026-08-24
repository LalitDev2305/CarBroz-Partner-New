package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.DesktopCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.DesktopPreferenceStoreProvider
import com.carbroz.data.securestorage.EphemeralDesktopSecureStorage
import java.io.File

/** Desktop host bridge using frozen credential policy and application-owned persistent storage. */
fun initializeCarBrozDesktopApplication(
    environment: String,
    apiBaseUrl: String,
    versionName: String,
    versionCode: Long,
    applicationId: String,
) {
    val appDirectory = File(System.getProperty("user.home"), ".carbroz/$applicationId")
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
                databaseDirectory = File(appDirectory, "database"),
            ),
        ),
        preferenceStoreProvider = DesktopPreferenceStoreProvider(
            storageDirectory = File(appDirectory, "preferences"),
        ),
    )
}
