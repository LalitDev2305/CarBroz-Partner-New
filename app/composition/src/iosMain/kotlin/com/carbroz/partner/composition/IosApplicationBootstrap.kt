package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.IosCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.IosPreferenceStoreProvider
import com.carbroz.data.securestorage.KeychainSecureStorage

/** Thin Swift-to-common bootstrap bridge with platform-backed storage adapters. */
fun initializeCarBrozIosApplication(
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
        secureStorage = KeychainSecureStorage(service = applicationId),
        databaseProvider = CarBrozDatabaseFactory(
            builderProvider = IosCarBrozDatabaseBuilderProvider(),
        ),
        preferenceStoreProvider = IosPreferenceStoreProvider(),
    )
}
