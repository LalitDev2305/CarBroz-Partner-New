package com.carbroz.partner.composition

import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.DesktopBackgroundScheduler
import com.carbroz.platform.background.DesktopContinuousExecutionController
import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.DesktopCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.DesktopPreferenceStoreProvider
import com.carbroz.data.securestorage.EphemeralDesktopSecureStorage
import org.koin.mp.KoinPlatform
import java.io.File

/** Desktop host bridge using frozen credential policy and explicit process-scoped background policy. */
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
        capabilityProviders = desktopCapabilityProviders(),
        backgroundScheduler = DesktopBackgroundScheduler(
            runnerProvider = { KoinPlatform.getKoin().get<BackgroundTaskRunner>() },
        ),
        continuousExecutionController = DesktopContinuousExecutionController(),
    )
}
