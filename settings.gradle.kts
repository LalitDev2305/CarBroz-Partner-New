pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CarBroz-Partner"

// Platform Hosts
include(":androidApp")
include(":desktopApp")

// Core Foundation Modules
include(":core:observability")
include(":core:ui")
include(":core:mvi")
include(":core:navigation")

// Domain Modules
include(":domain:session")

// Engine Modules
include(":engine:execution")

// SDUI Modules
include(":sdui:engine")
include(":sdui:render")
include(":sdui:runtime")
include(":sdui:host")

// Feature Modules
include(":feature:splash")

// Infrastructure Modules
include(":infrastructure:network")
include(":infrastructure:persistence")

// Application Composition Root
include(":app:composition")

