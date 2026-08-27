plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kover)
}

val consumePublishedFoundation = providers
    .gradleProperty("carbroz.foundation.consumePublished")
    .map(String::toBoolean)
    .orElse(false)
val foundationGroup = providers.gradleProperty("carbroz.foundation.group").get()
val foundationVersion = providers.gradleProperty("carbroz.foundation.version").get()

fun foundationDependency(module: String): Any =
    if (consumePublishedFoundation.get()) {
        "$foundationGroup:$module:$foundationVersion"
    } else {
        project(":foundation:$module")
    }

fun neutralDependency(projectPath: String, artifact: String): Any =
    if (consumePublishedFoundation.get()) {
        "$foundationGroup:$artifact:$foundationVersion"
    } else {
        project(projectPath)
    }

kotlin {
    android {
        namespace = "com.carbroz.partner.composition"
        compileSdk = 37
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
            api(foundationDependency("configuration"))
            implementation(project(":feature:splash"))
            implementation(project(":feature:dynamic"))
            implementation(foundationDependency("lifecycle"))
            implementation(foundationDependency("time"))
            implementation(foundationDependency("security"))
            implementation(foundationDependency("observability"))
            implementation(foundationDependency("session"))
            implementation(neutralDependency(":runtime:application", "application"))
            implementation(foundationDependency("architecture"))
            implementation(foundationDependency("navigation"))
            implementation(foundationDependency("adaptive"))
            implementation(foundationDependency("design-system"))
            implementation(foundationDependency("capabilities"))
            implementation(foundationDependency("analytics"))
            implementation(project(":platform:background"))
            implementation(project(":runtime:action"))
            implementation(project(":runtime:binding"))
            implementation(project(":runtime:sdui"))
            implementation(project(":data:network"))
            implementation(project(":data:realtime"))
            implementation(project(":data:sync"))
            implementation(project(":data:database"))
            implementation(project(":data:preferences"))
            implementation(project(":data:secure-storage"))
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
