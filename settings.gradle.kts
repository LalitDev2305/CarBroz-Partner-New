pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarBroz-Partner"

include(":androidApp")
include(":desktopApp")
include(":app:shared")
include(":foundation:architecture")
include(":foundation:lifecycle")
include(":foundation:configuration")
include(":foundation:time")
include(":foundation:localization")
include(":foundation:feature-control")
include(":runtime:application")
