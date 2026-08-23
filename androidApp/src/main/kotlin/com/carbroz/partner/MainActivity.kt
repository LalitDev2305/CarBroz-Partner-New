package com.carbroz.partner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.carbroz.partner.composition.CarBrozApp

/** Android host entry point. Reusable application behavior belongs in KMP modules. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CarBrozApp() }
    }
}
