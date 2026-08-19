plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            implementation(project(":sdui:render"))
            implementation(project(":sdui:engine"))
            implementation(project(":engine:execution"))
            implementation(project(":domain:actions"))
            implementation(project(":core:mvi"))
            implementation(project(":core:ui"))
            implementation(project(":core:observability"))
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
