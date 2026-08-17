package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Main Android activity serving as the thin host for the CarBroz Partner application.
 *
 * It delegates layout rendering directly to the shared [CarBrozPartnerRoot] Compose entry point
 * without containing business or state logic.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CarBrozPartnerRoot()
        }
    }
}

