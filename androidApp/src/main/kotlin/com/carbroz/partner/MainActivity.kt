package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.carbroz.partner.composition.AppLifecycleBridge
import com.carbroz.partner.composition.CarBrozApp
import com.carbroz.partner.composition.createCarBrozAppConfiguration
import com.carbroz.partner.composition.initializeCarBrozDependencyInjection

/** Android host entry point. Reusable application behavior belongs in KMP modules. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeCarBrozDependencyInjection(
            configuration = createCarBrozAppConfiguration(
                environment = BuildConfig.CARBROZ_ENVIRONMENT,
                apiBaseUrl = BuildConfig.CARBROZ_API_BASE_URL,
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE.toLong(),
                applicationId = BuildConfig.APPLICATION_ID,
            ),
        )
        setContent { CarBrozApp() }
    }

    override fun onStart() {
        super.onStart()
        AppLifecycleBridge.moveToForeground()
    }

    override fun onStop() {
        AppLifecycleBridge.moveToBackground()
        super.onStop()
    }
}
