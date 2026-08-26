plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.carbroz.runtime.action"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":foundation:capabilities"))
            implementation(project(":runtime:binding"))
            implementation(project(":runtime:form"))
            implementation(project(":runtime:sdui"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
