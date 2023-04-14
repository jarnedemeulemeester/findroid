plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.player.video"
    compileSdk = 33
    buildToolsVersion = "33.0.2"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(false)
    disabledRules.add("max-line-length")
}

dependencies {
    implementation(project(":player:core"))
    implementation(project(":data"))
    implementation(project(":preferences"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.libmpv)
    implementation(libs.material)
    implementation(libs.timber)
}
