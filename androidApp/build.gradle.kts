plugins {
    alias(libs.plugins.androidApplication)
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
}

dependencies {
    implementation(project(":app:composition"))
    implementation(libs.androidx.activity.compose)
}
