pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

val consumePublishedFoundation = providers
    .gradleProperty("carbroz.foundation.consumePublished")
    .map(String::toBoolean)
    .orElse(false)

val foundationGroup = providers.gradleProperty("carbroz.foundation.group").get()

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (consumePublishedFoundation.get()) {
            maven {
                name = "foundationLocal"
                url = rootDir.resolve("build/foundation-repository").toURI()
                content {
                    includeGroup(foundationGroup)
                }
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "CarBroz-Partner"

include(":androidApp")
include(":desktopApp")
include(":app:composition")
include(":feature:splash")
include(":feature:dynamic")
include(":foundation:architecture")
include(":foundation:lifecycle")
include(":foundation:configuration")
include(":foundation:time")
include(":foundation:localization")
include(":foundation:security")
include(":foundation:session")
include(":foundation:adaptive")
include(":foundation:design-system")
include(":foundation:navigation")
include(":foundation:capabilities")
include(":foundation:observability")
include(":foundation:analytics")
include(":runtime:application")
include(":runtime:sdui")
include(":runtime:binding")
include(":runtime:action")
include(":data:bootstrap")
include(":data:database")
include(":data:network")
include(":data:preferences")
include(":data:secure-storage")
include(":data:realtime")
include(":data:sync")
include(":platform:background")
