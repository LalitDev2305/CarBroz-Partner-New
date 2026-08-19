package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.carbroz.partner.app.composition.CarBrozPartnerRoot

/**
 * Android Platform Host Activity.
 *
 * Serves as the thin Android entry point and delegates layout rendering directly to [CarBrozPartnerRoot].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarBrozPartnerRoot()
        }
    }
}
