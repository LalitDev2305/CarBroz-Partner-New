package com.carbroz.partner.composition

import android.content.Context
import com.carbroz.data.database.AndroidCarBrozDatabaseBuilderProvider
import com.carbroz.data.database.CarBrozDatabaseFactory
import com.carbroz.data.securestorage.AndroidKeystoreSecureStorage

/** Android host bridge that supplies platform-backed secure storage and database adapters. */
fun initializeCarBrozAndroidApplication(
    context: Context,
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
    initializeCarBrozDependencyInjection(
        configuration = configuration,
        secureStorage = AndroidKeystoreSecureStorage(
            context = context,
            namespace = applicationId,
        ),
        databaseProvider = CarBrozDatabaseFactory(
            builderProvider = AndroidCarBrozDatabaseBuilderProvider(context),
        ),
    )
}
