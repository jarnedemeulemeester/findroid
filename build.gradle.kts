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
        classpath("com.android.tools.build:gradle:7.2.1")

        val kotlinVersion = "1.7.0"
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
        val navVersion = "2.4.2"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")

        val hiltVersion = "2.42"
        classpath("com.google.dagger:hilt-android-gradle-plugin:$hiltVersion")

        val aboutLibrariesVersion = "10.3.0"
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