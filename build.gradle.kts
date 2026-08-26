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
    if (neutralFoundationPrefixes.any { prefix -> path.startsWith(prefix) }) {
        group = foundationGroup
        version = foundationVersion

        pluginManager.apply("maven-publish")
        plugins.withId("maven-publish") {
            extensions.configure<org.gradle.api.publish.PublishingExtension> {
                repositories {
                    maven {
                        name = "foundationLocal"
                        url = rootProject.layout.buildDirectory.dir("foundation-repository").get().asFile.toURI()
                    }
                }
            }
        }
    }
}

val foundationCoordinateDrift = provider {
    subprojects
        .filter { project -> neutralFoundationPrefixes.any { prefix -> project.path.startsWith(prefix) } }
        .filter { project ->
            project.group.toString() != foundationGroup || project.version.toString() != foundationVersion
        }
        .map { project -> project.path }
}

if (foundationCoordinateDrift.get().isNotEmpty()) {
    error("Neutral foundation coordinate drift: ${foundationCoordinateDrift.get().joinToString()}")
}

tasks.register("verifyFoundationCoordinates") {
    group = "verification"
    description = "Verifies canonical coordinates for neutral CarBroz foundation modules."
}

tasks.register("publishFoundationToLocalRepository") {
    group = "publishing"
    description = "Publishes neutral foundation modules to the build-local Maven repository."
    dependsOn(
        subprojects
            .filter { project -> neutralFoundationPrefixes.any { prefix -> project.path.startsWith(prefix) } }
            .map { project -> "${project.path}:publishAllPublicationsToFoundationLocalRepository" },
    )
}
