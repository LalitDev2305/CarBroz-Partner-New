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
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "appComposition"
            isStatic = true
        }
    }

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

        val iosArm64Main by getting {
            dependencies {
                implementation(project(":infrastructure:persistence"))
            }
        }

        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(project(":infrastructure:persistence"))
            }
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
