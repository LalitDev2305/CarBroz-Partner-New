plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.carbroz.partner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.carbroz.partner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":app:composition"))
    implementation(project(":domain:session"))
    implementation(project(":infrastructure:persistence"))
    implementation(libs.androidx.activity.compose)
}
