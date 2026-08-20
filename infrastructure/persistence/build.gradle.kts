plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    androidLibrary {
        namespace = "com.carbroz.partner.infrastructure.persistence"
        compileSdk = 36
        minSdk = 24
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain:session"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.security.crypto)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
