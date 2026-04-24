pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "findroid"

include(":app:phone")
include(":app:tv")
include(":core")
include(":data")
include(":player:core")
include(":player:local")
include(":setup")
include(":modes:film")
include(":settings")