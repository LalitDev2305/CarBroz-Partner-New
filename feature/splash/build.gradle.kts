plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.carbroz.feature.splash"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":foundation:architecture"))
            // Splash's public bootstrap handoff exposes DynamicScreenInstruction/Codec types.
            api(project(":feature:dynamic"))
            // PreferenceBackedBootstrapConfigurationCache is a public adapter over this typed store.
            api(project(":data:preferences"))
            implementation(project(":foundation:lifecycle"))
            implementation(project(":foundation:navigation"))
            implementation(project(":foundation:design-system"))
            implementation(project(":runtime:application"))
            implementation(project(":data:network"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(project(":runtime:sdui"))
        }
    }
}
