package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.IosCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.IosPreferenceStoreProvider
import com.carbroz.data.securestorage.KeychainSecureStorage
import com.carbroz.foundation.observability.IosMainThreadDispatcher
import com.carbroz.foundation.observability.IosPlatformDiagnosticSink
import com.carbroz.foundation.observability.IosResourceDiagnostics
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.IosBackgroundScheduler
import com.carbroz.platform.background.IosContinuousExecutionController
import com.carbroz.platform.background.iosPermittedBackgroundTaskIds
import org.koin.mp.KoinPlatform

/** Thin Swift-to-common bootstrap bridge with platform-backed storage, execution and diagnostics adapters. */
fun initializeCarBrozIosApplication(
    environment: String,
    apiBaseUrl: String,
    versionName: String,
    versionCode: Long,
    applicationId: String,
) {
    val configuration = createCarBrozAppConfiguration(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        versionName = versionName,
        versionCode = versionCode,
        applicationId = applicationId,
    )
    val diagnosticSink = IosPlatformDiagnosticSink()
    val localDiagnostics = localOperationalDiagnostics(
        environment = configuration.environment,
        platformSinks = ObservabilitySinks(
            log = diagnosticSink,
            crash = diagnosticSink,
            performance = diagnosticSink,
            trace = diagnosticSink,
            responsiveness = diagnosticSink,
            resource = diagnosticSink,
        ),
        resourceDiagnostics = IosResourceDiagnostics(),
        mainThreadDispatcher = IosMainThreadDispatcher(),
    )

    initializeCarBrozDependencyInjection(
        configuration = configuration,
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
        observabilitySinks = localDiagnostics.sinks,
        resourceDiagnostics = localDiagnostics.resourceDiagnostics,
        mainThreadDispatcher = localDiagnostics.mainThreadDispatcher,
    )
}
