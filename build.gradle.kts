import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency

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
val productSpecificProjectPaths = setOf(":data:bootstrap")
val foundationLocalRepository = layout.buildDirectory.dir("foundation-repository")

fun Project.isNeutralFoundationProject(): Boolean =
    neutralFoundationPrefixes.any { prefix -> path.startsWith(prefix) } && path !in productSpecificProjectPaths

fun Project.commonMainProjectDependencyPaths(): Set<String> =
    sequenceOf("commonMainApi", "commonMainImplementation")
        .mapNotNull { configurationName -> configurations.findByName(configurationName) }
        .flatMap { configuration ->
            configuration.dependencies
                .withType(ProjectDependency::class.java)
                .asSequence()
        }
        .map { dependency -> dependency.path }
        .toSet()

val cleanFoundationLocalRepository = tasks.register<Delete>("cleanFoundationLocalRepository") {
    group = "publishing"
    description = "Removes the build-local foundation repository before a verification publication."
    delete(foundationLocalRepository)
}

subprojects {
    if (isNeutralFoundationProject()) {
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

            tasks.withType<org.gradle.api.publish.maven.tasks.PublishToMavenRepository>().configureEach {
                mustRunAfter(rootProject.tasks.named("cleanFoundationLocalRepository"))
            }
        }
    }
}

val foundationCoordinateDrift = provider {
    subprojects
        .filter { project -> project.isNeutralFoundationProject() }
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

val requiredStartupDependencies = mapOf(
    ":feature:splash" to setOf(
        ":foundation:architecture",
        ":foundation:lifecycle",
        ":foundation:navigation",
        ":foundation:design-system",
        ":runtime:application",
    ),
    ":runtime:application" to setOf(
        ":foundation:lifecycle",
        ":foundation:observability",
        ":foundation:session",
        ":foundation:time",
    ),
    ":data:bootstrap" to setOf(
        ":runtime:application",
        ":data:network",
    ),
)

val forbiddenStartupDependencies = mapOf(
    ":feature:splash" to setOf(
        ":data:network",
        ":data:bootstrap",
        ":foundation:session",
        ":feature:dynamic",
        ":app:composition",
    ),
    ":runtime:application" to setOf(
        ":data:network",
        ":data:bootstrap",
        ":feature:splash",
        ":feature:dynamic",
        ":app:composition",
    ),
    ":data:network" to setOf(
        ":runtime:application",
        ":data:bootstrap",
        ":feature:splash",
        ":feature:dynamic",
        ":app:composition",
    ),
    ":data:bootstrap" to setOf(
        ":foundation:session",
        ":feature:splash",
        ":feature:dynamic",
        ":app:composition",
    ),
)

tasks.register("verifyStartupArchitecture") {
    group = "verification"
    description = "Enforces the frozen Splash/bootstrap dependency and ownership boundaries."

    doLast {
        val violations = mutableListOf<String>()

        requiredStartupDependencies.forEach { (projectPath, required) ->
            val actual = project(projectPath).commonMainProjectDependencyPaths()
            (required - actual).sorted().forEach { missing ->
                violations += "$projectPath is missing required commonMain dependency $missing"
            }
        }

        forbiddenStartupDependencies.forEach { (projectPath, forbidden) ->
            val actual = project(projectPath).commonMainProjectDependencyPaths()
            (actual intersect forbidden).sorted().forEach { dependency ->
                violations += "$projectPath must not depend on $dependency"
            }
        }

        subprojects
            .filter { foundationProject -> foundationProject.path.startsWith(":foundation:") }
            .forEach { foundationProject ->
                if (":data:bootstrap" in foundationProject.commonMainProjectDependencyPaths()) {
                    violations += "${foundationProject.path} must not depend on Partner-specific :data:bootstrap"
                }
            }

        if (project.findProject(":app:startup") != null) {
            violations += ":app:startup is superseded and must not exist"
        }

        if (violations.isNotEmpty()) {
            error("Startup architecture boundary violations:\n${violations.joinToString(separator = "\n") { " - $it" }}")
        }
    }
}

tasks.register("publishFoundationToLocalRepository") {
    group = "publishing"
    description = "Publishes neutral foundation modules to a freshly cleaned build-local Maven repository."
    dependsOn(cleanFoundationLocalRepository)
    dependsOn(
        subprojects
            .filter { project -> project.isNeutralFoundationProject() }
            .map { project -> "${project.path}:publishAllPublicationsToFoundationLocalRepository" },
    )
}
