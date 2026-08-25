package com.carbroz.partner.composition

import android.content.Context
import com.carbroz.capabilities.background.AndroidBackgroundScheduler
import com.carbroz.capabilities.background.AndroidContinuousExecutionController
import com.carbroz.data.database.AndroidCarBrozDatabaseBuilderProvider
import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.preferences.AndroidPreferenceStoreProvider
import com.carbroz.data.securestorage.AndroidKeystoreSecureStorage

/** Android host bridge that supplies platform-backed storage, capability and execution adapters. */
fun initializeCarBrozAndroidApplication(
    context: Context,
    environment: String,
    apiBaseUrl: String,
    versionName: String,
    versionCode: Long,
    applicationId: String,
) {
    val appContext = context.applicationContext
    val configuration = createCarBrozAppConfiguration(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        versionName = versionName,
        versionCode = versionCode,
        applicationId = applicationId,
    )
    initializeCarBrozDependencyInjection(
        configuration = configuration,
        secureStorage = AndroidKeystoreSecureStorage(
            context = appContext,
            namespace = applicationId,
        ),
        databaseProvider = CarBrozDatabaseFactory(
            builderProvider = AndroidCarBrozDatabaseBuilderProvider(appContext),
        ),
        preferenceStoreProvider = AndroidPreferenceStoreProvider(appContext),
        capabilityProviders = androidCapabilityProviders(appContext),
        backgroundScheduler = AndroidBackgroundScheduler(appContext),
        continuousExecutionController = AndroidContinuousExecutionController(appContext),
    )
}
