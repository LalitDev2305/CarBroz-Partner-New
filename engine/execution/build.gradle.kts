plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    )

    jvm("desktop")
    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain:session"))
            implementation(project(":infrastructure:network"))
            implementation(project(":core:observability"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(project(":domain:storage"))
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
