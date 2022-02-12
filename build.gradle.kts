// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.1.0")

        val kotlinVersion = "1.6.10"
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
        val navVersion = "2.5.0-alpha01"
        // NOTE: 2.5.0-alpha01 is used because 2.4.0 is incompatible with AGP 7.1
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")

        val hiltVersion = "2.40.5"
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")

        val aboutLibrariesVersion = "8.9.4"
        classpath("com.mikepenz.aboutlibraries.plugin:aboutlibraries-plugin:$aboutLibrariesVersion")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

tasks.create<Delete>("clean") {
    delete(rootProject.buildDir)
}