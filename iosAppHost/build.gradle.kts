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

    sourceSets {
        commonMain.dependencies {
            implementation(project(":app:composition"))
            implementation(project(":domain:session"))
            implementation(compose.runtime)
            implementation(compose.ui)
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
    }
}
