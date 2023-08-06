plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.androidx.navigation.safeargs)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.core"
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

    flavorDimensions += "variant"
    productFlavors {
        register("libre")
        register("huawei")
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
    implementation(project(":player:core"))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.material)
    implementation(libs.timber)
}
