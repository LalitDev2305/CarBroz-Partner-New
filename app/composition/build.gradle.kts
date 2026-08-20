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
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(project(":sdui:host"))
            implementation(project(":sdui:render"))
            implementation(project(":sdui:runtime"))
            implementation(project(":sdui:engine"))
            implementation(project(":engine:execution"))
            implementation(project(":infrastructure:network"))
            implementation(project(":core:navigation"))
            implementation(project(":core:observability"))
            implementation(project(":domain:session"))
            implementation(project(":feature:splash"))
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
