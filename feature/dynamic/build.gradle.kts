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
        namespace = "com.carbroz.feature.dynamic"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":foundation:configuration"))
            implementation(project(":foundation:lifecycle"))
            implementation(project(":foundation:navigation"))
            implementation(project(":foundation:capabilities"))
            implementation(project(":foundation:session"))
            implementation(project(":runtime:action"))
            implementation(project(":runtime:binding"))
            // DynamicScreenInstruction/Destination publicly expose canonical SDUI model types.
            api(project(":runtime:sdui"))
            implementation(project(":data:network"))
            implementation(project(":data:realtime"))
            implementation(project(":platform:background"))
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
        }
    }
}
