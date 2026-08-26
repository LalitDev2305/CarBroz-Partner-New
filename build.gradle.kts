plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
}

val foundationGroup = providers.gradleProperty("carbroz.foundation.group").get()
val foundationVersion = providers.gradleProperty("carbroz.foundation.version").get()
val neutralFoundationPrefixes = listOf(":foundation:", ":runtime:", ":data:", ":platform:")

subprojects {
    if (neutralFoundationPrefixes.any(path::startsWith)) {
        group = foundationGroup
        version = foundationVersion
    }
}

tasks.register("verifyFoundationCoordinates") {
    group = "verification"
    description = "Verifies canonical coordinates for neutral CarBroz foundation modules."

    doLast {
        val drift = subprojects.filter { project ->
            neutralFoundationPrefixes.any(project.path::startsWith) &&
                (project.group.toString() != foundationGroup || project.version.toString() != foundationVersion)
        }
        require(drift.isEmpty()) {
            "Neutral foundation coordinate drift: " + drift.joinToString { it.path }
        }
    }
}
