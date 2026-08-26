plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

val developmentApiBaseUrl = providers.gradleProperty("carbroz.api.development")
    .orElse("https://development.invalid")
val stagingApiBaseUrl = providers.gradleProperty("carbroz.api.staging")
    .orElse("https://staging.invalid")
val productionApiBaseUrl = providers.gradleProperty("carbroz.api.production")
    .orElse("https://production.invalid")

android {
    namespace = "com.carbroz.partner"
    compileSdk = 37

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.carbroz.partner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "CARBROZ_ENVIRONMENT", "\"development\"")
            buildConfigField("String", "CARBROZ_API_BASE_URL", "\"${developmentApiBaseUrl.get()}\"")
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "CARBROZ_ENVIRONMENT", "\"staging\"")
            buildConfigField("String", "CARBROZ_API_BASE_URL", "\"${stagingApiBaseUrl.get()}\"")
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "CARBROZ_ENVIRONMENT", "\"production\"")
            buildConfigField("String", "CARBROZ_API_BASE_URL", "\"${productionApiBaseUrl.get()}\"")
        }
    }
}

dependencies {
    implementation(project(":app:composition"))
    implementation(libs.androidx.activity.compose)
}
