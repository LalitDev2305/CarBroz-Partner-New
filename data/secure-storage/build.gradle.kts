plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.carbroz.data.securestorage"
        compileSdk = 36
        minSdk = 24
        withHostTest {}
    }

    jvm("desktop")
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":foundation:security"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
