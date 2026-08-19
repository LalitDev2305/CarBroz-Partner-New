package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.carbroz.partner.app.composition.CarBrozPartnerRoot
import com.carbroz.partner.app.composition.config.AppConfig

/**
 * Android Platform Host Activity.
 *
 * Serves as the thin Android entry point. Instantiates [AppConfig] with environment
 * parameters and delegates layout rendering directly to [CarBrozPartnerRoot].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AppConfig(
            baseUrl = "https://api.carbroz.com"
        )
        setContent {
            CarBrozPartnerRoot(config = config)
        }
    }
}
