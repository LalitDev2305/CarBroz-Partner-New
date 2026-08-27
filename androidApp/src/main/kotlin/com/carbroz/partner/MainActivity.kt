package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.carbroz.partner.composition.AppLifecycleBridge
import com.carbroz.partner.composition.CarBrozApp
import com.carbroz.partner.composition.NavigationProcessStateBridge
import com.carbroz.partner.composition.initializeCarBrozAndroidApplication

/** Android host entry point. Reusable application behavior belongs in KMP modules. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeCarBrozAndroidApplication(
            context = applicationContext,
            environment = BuildConfig.CARBROZ_ENVIRONMENT,
            apiBaseUrl = BuildConfig.CARBROZ_API_BASE_URL,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            applicationId = BuildConfig.APPLICATION_ID,
        )
        NavigationProcessStateBridge.restore(savedInstanceState?.getString(NAVIGATION_PROCESS_STATE_KEY))
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

    override fun onSaveInstanceState(outState: Bundle) {
        NavigationProcessStateBridge.save()?.let {
            outState.putString(NAVIGATION_PROCESS_STATE_KEY, it)
        }
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val NAVIGATION_PROCESS_STATE_KEY = "carbroz.dynamic.navigation.process_state"
    }
}
