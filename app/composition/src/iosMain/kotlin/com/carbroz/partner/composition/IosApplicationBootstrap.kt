package com.carbroz.partner.composition

import com.carbroz.capabilities.background.BackgroundTaskRunner
import com.carbroz.capabilities.background.IosBackgroundScheduler
import com.carbroz.capabilities.background.IosContinuousExecutionController
import com.carbroz.capabilities.background.iosPermittedBackgroundTaskIds
import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.IosCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.IosPreferenceStoreProvider
import com.carbroz.data.securestorage.KeychainSecureStorage
import org.koin.mp.KoinPlatform

/** Thin Swift-to-common bootstrap bridge with platform-backed storage, capability and execution adapters. */
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
        capabilityProviders = iosCapabilityProviders(),
        backgroundScheduler = IosBackgroundScheduler(
            permittedTaskIds = iosPermittedBackgroundTaskIds(),
            runnerProvider = { KoinPlatform.getKoin().get<BackgroundTaskRunner>() },
        ),
        continuousExecutionController = IosContinuousExecutionController(),
    )
}
