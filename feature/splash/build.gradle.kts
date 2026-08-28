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
            api(project(":foundation:lifecycle"))
            api(project(":foundation:navigation"))
            api(project(":runtime:application"))
            api(project(":data:network"))
            api(project(":data:preferences"))
            // Splash's public bootstrap handoff exposes DynamicScreenInstruction/Codec types.
            api(project(":feature:dynamic"))
            // Bootstrap/Splash public contracts expose StateFlow, CoroutineScope and JsonObject/Json.
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)

            implementation(project(":foundation:design-system"))
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
