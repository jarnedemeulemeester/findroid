@Suppress("DSL_SCOPE_VIOLATION") // False positive
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.player.core"
    compileSdk = 33
    buildToolsVersion = "33.0.1"

    defaultConfig {
        minSdk = 27
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
        create("staging") {
            initWith(getByName("release"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    disabledRules.add("max-line-length")
}

dependencies {
    implementation(project(":data"))
    implementation(project(":preferences"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jellyfin.core)
    implementation(libs.timber)
}
