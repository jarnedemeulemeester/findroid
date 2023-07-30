plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        applicationId = "dev.jdtech.jellyfin"
        minSdk = 27
        targetSdk = 33

        val appVersionCode: Int by rootProject.extra
        val appVersionName: String by rootProject.extra

        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildTypes {
        named("debug") {
            applicationIdSuffix = ".debug"
        }
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        register("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
        }
    }

    flavorDimensions += "variant"
    productFlavors {
        register("libre") {
            dimension = "variant"
            isDefault = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ktlint {
    version.set("0.50.0")
    android.set(true)
    ignoreFailures.set(false)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":preferences"))
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
