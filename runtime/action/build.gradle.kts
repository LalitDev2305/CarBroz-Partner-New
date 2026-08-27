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
            // PreparedAction.Capability is part of the public semantic action contract.
            api(project(":foundation:capabilities"))
            // ActionPreparationContext publicly exposes BindingContext.
            api(project(":runtime:binding"))
            // PreparedAction and ActionPreparationContext publicly expose SDUI/form-template types.
            api(project(":runtime:sdui"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
