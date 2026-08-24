plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

kotlin {
    android {
        namespace = "com.carbroz.partner.composition"
        compileSdk = 36
        minSdk = 24
        withHostTest {}
    }

    jvm("desktop")

    iosArm64 {
        binaries.framework {
            baseName = "CarBrozShared"
            isStatic = true
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "CarBrozShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // AppConfiguration is part of composition's public host-facing API.
            api(project(":foundation:configuration"))
            implementation(project(":foundation:lifecycle"))
            implementation(project(":foundation:time"))
            implementation(project(":foundation:security"))
            implementation(project(":foundation:session"))
            implementation(project(":foundation:navigation"))
            implementation(project(":foundation:adaptive"))
            implementation(project(":foundation:design-system"))
            implementation(project(":runtime:application"))
            implementation(project(":runtime:action"))
            implementation(project(":runtime:sdui"))
            implementation(project(":data:network"))
            implementation(project(":data:database"))
            implementation(project(":data:preferences"))
            implementation(project(":data:secure-storage"))
            // PreparedAction.Request exposes JsonObject in its public payload type, so composition
            // must have the serialization JSON API on its own compile classpath.
            implementation(libs.kotlinx.serialization.json)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.test)
        }
    }
}
