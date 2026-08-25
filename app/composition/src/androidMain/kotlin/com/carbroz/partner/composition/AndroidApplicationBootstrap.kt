package com.carbroz.partner.composition

import android.content.Context
import com.carbroz.data.database.AndroidCarBrozDatabaseBuilderProvider
import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.preferences.AndroidPreferenceStoreProvider
import com.carbroz.data.securestorage.AndroidKeystoreSecureStorage
import com.carbroz.foundation.observability.AndroidMainThreadDispatcher
import com.carbroz.foundation.observability.AndroidPlatformDiagnosticSink
import com.carbroz.foundation.observability.AndroidResourceDiagnostics
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.platform.background.AndroidBackgroundScheduler
import com.carbroz.platform.background.AndroidContinuousExecutionController

/** Android host bridge that supplies platform-backed storage, capability, execution and diagnostic adapters. */
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
    val localDiagnostics = localOperationalDiagnostics(
        environment = configuration.environment,
        platformSinks = {
            val diagnosticSink = AndroidPlatformDiagnosticSink()
            ObservabilitySinks(
                log = diagnosticSink,
                crash = diagnosticSink,
                performance = diagnosticSink,
                trace = diagnosticSink,
                responsiveness = diagnosticSink,
                resource = diagnosticSink,
            )
        },
        resourceDiagnostics = { AndroidResourceDiagnostics() },
        mainThreadDispatcher = { AndroidMainThreadDispatcher() },
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
        observabilitySinks = localDiagnostics.sinks,
        resourceDiagnostics = localDiagnostics.resourceDiagnostics,
        mainThreadDispatcher = localDiagnostics.mainThreadDispatcher,
    )
}
