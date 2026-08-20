plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main").defaultSourceSet.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    jvm("desktop") {
        compilations.getByName("main").defaultSourceSet.dependencies {
            implementation(libs.ktor.client.cio)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain:session"))
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
