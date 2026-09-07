package com.carbroz.partner.composition

import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.database.DesktopCarBrozDatabaseBuilderProvider
import com.carbroz.data.preferences.DesktopPreferenceStoreProvider
import com.carbroz.data.securestorage.EphemeralDesktopSecureStorage
import com.carbroz.foundation.configuration.ClientPlatform
import com.carbroz.foundation.observability.DesktopMainThreadDispatcher
import com.carbroz.foundation.observability.DesktopPlatformDiagnosticSink
import com.carbroz.foundation.observability.DesktopResourceDiagnostics
import com.carbroz.foundation.observability.ObservabilitySinks
import com.carbroz.platform.background.BackgroundTaskRunner
import com.carbroz.platform.background.DesktopBackgroundScheduler
import com.carbroz.platform.background.DesktopContinuousExecutionController
import org.koin.mp.KoinPlatform
import java.io.File

/** Desktop host bridge using frozen credential policy and explicit process-scoped execution/diagnostic policy. */
fun initializeCarBrozDesktopApplication(
    environment: String,
    apiBaseUrl: String,
    versionName: String,
    versionCode: Long,
    applicationId: String,
) {
    val appDirectory = File(System.getProperty("user.home"), ".carbroz/$applicationId")
    val configuration = createCarBrozAppConfiguration(
        environment = environment,
        apiBaseUrl = apiBaseUrl,
        clientPlatform = ClientPlatform.DESKTOP,
        versionName = versionName,
        versionCode = versionCode,
        applicationId = applicationId,
    )
    val localDiagnostics = localOperationalDiagnostics(
        environment = configuration.environment,
        platformSinks = {
            val diagnosticSink = DesktopPlatformDiagnosticSink()
            ObservabilitySinks(
                log = diagnosticSink,
                crash = diagnosticSink,
                performance = diagnosticSink,
                trace = diagnosticSink,
                responsiveness = diagnosticSink,
                resource = diagnosticSink,
            )
        },
        resourceDiagnostics = { DesktopResourceDiagnostics() },
        mainThreadDispatcher = { DesktopMainThreadDispatcher() },
    )

    initializeCarBrozDependencyInjection(
        configuration = configuration,
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
        observabilitySinks = localDiagnostics.sinks,
        resourceDiagnostics = localDiagnostics.resourceDiagnostics,
        mainThreadDispatcher = localDiagnostics.mainThreadDispatcher,
    )
}
