import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

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

abstract class VerifyStartupArchitectureTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val architectureFiles: ConfigurableFileCollection

    @TaskAction
    fun verifyArchitecture() {
        val contentsByPath = architectureFiles.files.associate { file ->
            file.invariantSeparatorsPath to file.readText()
        }

        fun content(relativePath: String): String = contentsByPath.entries
            .singleOrNull { (path, _) -> path.endsWith("/$relativePath") || path == relativePath }
            ?.value
            ?: error("Architecture verification input is missing: $relativePath")

        fun hasProjectDependency(buildFile: String, dependencyPath: String): Boolean {
            val escapedPath = Regex.escape(dependencyPath)
            return Regex("""project\s*\(\s*(?:path\s*=\s*)?[\"']$escapedPath[\"']""")
                .containsMatchIn(buildFile)
        }

        val moduleBuildFiles = mapOf(
            ":feature:splash" to content("feature/splash/build.gradle.kts"),
            ":feature:dynamic" to content("feature/dynamic/build.gradle.kts"),
            ":runtime:application" to content("runtime/application/build.gradle.kts"),
            ":data:network" to content("data/network/build.gradle.kts"),
            ":data:bootstrap" to content("data/bootstrap/build.gradle.kts"),
        )

        val requiredDependencies = mapOf(
            ":feature:splash" to setOf(
                ":foundation:architecture",
                ":foundation:lifecycle",
                ":foundation:navigation",
                ":foundation:design-system",
                ":runtime:application",
            ),
            ":runtime:application" to setOf(
                ":foundation:observability",
                ":foundation:session",
                ":foundation:time",
            ),
            ":data:bootstrap" to setOf(
                ":runtime:application",
                ":data:network",
            ),
        )

        val forbiddenDependencies = mapOf(
            ":feature:splash" to setOf(
                ":data:network",
                ":data:bootstrap",
                ":foundation:session",
                ":feature:dynamic",
                ":app:composition",
            ),
            ":feature:dynamic" to setOf(
                ":runtime:application",
            ),
            ":runtime:application" to setOf(
                ":foundation:lifecycle",
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

        val violations = mutableListOf<String>()

        requiredDependencies.forEach { (module, required) ->
            val buildFile = moduleBuildFiles.getValue(module)
            required.sorted().forEach { dependency ->
                if (!hasProjectDependency(buildFile, dependency)) {
                    violations += "$module is missing required commonMain dependency $dependency"
                }
            }
        }

        forbiddenDependencies.forEach { (module, forbidden) ->
            val buildFile = moduleBuildFiles.getValue(module)
            forbidden.sorted().forEach { dependency ->
                if (hasProjectDependency(buildFile, dependency)) {
                    violations += "$module must not depend on $dependency"
                }
            }
        }

        contentsByPath
            .filterKeys { path ->
                path.replace('\\', '/').contains("/foundation/") && path.endsWith("/build.gradle.kts")
            }
            .forEach { (path, buildFile) ->
                if (hasProjectDependency(buildFile, ":data:bootstrap")) {
                    violations += "$path must not depend on Partner-specific :data:bootstrap"
                }
            }

        val settings = content("settings.gradle.kts")
        if (Regex("""include\s*\(\s*[\"']:app:startup[\"']\s*\)""").containsMatchIn(settings)) {
            violations += ":app:startup is superseded and must not exist"
        }

        if (violations.isNotEmpty()) {
            error(
                "Startup architecture boundary violations:\n" +
                    violations.joinToString(separator = "\n") { " - $it" },
            )
        }
    }
}

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

tasks.register<VerifyStartupArchitectureTask>("verifyStartupArchitecture") {
    group = "verification"
    description = "Enforces the frozen Splash/bootstrap dependency and ownership boundaries."
    architectureFiles.from(
        layout.projectDirectory.file("settings.gradle.kts"),
        layout.projectDirectory.file("feature/splash/build.gradle.kts"),
        layout.projectDirectory.file("feature/dynamic/build.gradle.kts"),
        layout.projectDirectory.file("runtime/application/build.gradle.kts"),
        layout.projectDirectory.file("data/network/build.gradle.kts"),
        layout.projectDirectory.file("data/bootstrap/build.gradle.kts"),
        layout.projectDirectory.asFileTree.matching {
            include("foundation/*/build.gradle.kts")
        },
    )
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
