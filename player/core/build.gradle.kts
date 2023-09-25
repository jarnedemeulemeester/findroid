plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.player.core"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 28
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
        }
        register("staging") {
            initWith(getByName("release"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ktlint {
    version.set("0.50.0")
    android.set(true)
    ignoreFailures.set(false)
}

dependencies {
    implementation(project(":data"))
    implementation(project(":preferences"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.preference)
    implementation(libs.jellyfin.core)
    implementation(libs.timber)
}
