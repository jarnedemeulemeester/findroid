import com.android.build.api.dsl.CommonExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
@Suppress("DSL_SCOPE_VIOLATION") // False positive
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.androidx.navigation.safeargs) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.ktlint) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }

    val configureAndroid = { _: AppliedPlugin ->
        extensions.configure<CommonExtension<*, *, *, *>>("android") {
            lint {
                informational += "MissingTranslation"
            }
        }
    }

    pluginManager.withPlugin("com.android.library", configureAndroid)
    pluginManager.withPlugin("com.android.application", configureAndroid)
}

tasks.create<Delete>("clean") {
    delete(rootProject.buildDir)
}

val appVersionCode by extra { 17 }
val appVersionName by extra { "0.10.1" }