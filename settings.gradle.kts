enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app:phone")
include(":app:tv")
include(":core")
include(":data")
include(":preferences")
include(":player:core")
include(":player:video")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}