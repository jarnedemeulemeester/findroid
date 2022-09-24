plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
    id("dagger.hilt.android.plugin")
    id("com.mikepenz.aboutlibraries.plugin")
}

android {
    namespace = "dev.jdtech.jellyfin"
    compileSdk = 33
    buildToolsVersion = "33.0.0"

    defaultConfig {
        applicationId = "dev.jdtech.jellyfin"
        minSdk = 26
        targetSdk = 33
        versionCode = 12
        versionName = "0.6.1"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.leanback:leanback:1.2.0-alpha02")

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")


    // Material
    implementation("com.google.android.material:material:1.6.1")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle
    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")

    // Navigation
    val navigationVersion = "2.5.2"
    implementation("androidx.navigation:navigation-fragment-ktx:$navigationVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navigationVersion")

    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    // Room
    val roomVersion = "2.4.3"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // Preference
    val preferenceVersion = "1.2.0"
    implementation("androidx.preference:preference-ktx:$preferenceVersion")

    // Jellyfin
    val jellyfinVersion = "1.3.5"
    implementation("org.jellyfin.sdk:jellyfin-core:$jellyfinVersion")

    // Glide
    val glideVersion = "4.13.2"
    implementation("com.github.bumptech.glide:glide:$glideVersion")
    kapt("com.github.bumptech.glide:compiler:$glideVersion")

    // Hilt
    val hiltVersion = "2.44"
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")

    // ExoPlayer
    val exoplayerVersion = "2.18.1"
    implementation("com.google.android.exoplayer:exoplayer-core:$exoplayerVersion")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoplayerVersion")
    implementation(files("libs/extension-ffmpeg-release.aar"))

    // MPV
    implementation(files("libs/libmpv.aar"))

    // Timber
    val timberVersion = "5.0.1"
    implementation("com.jakewharton.timber:timber:$timberVersion")

    val aboutLibrariesVersion = "10.4.0"
    implementation("com.mikepenz:aboutlibraries-core:$aboutLibrariesVersion")
    implementation("com.mikepenz:aboutlibraries:$aboutLibrariesVersion")

    val pagingVersion = "3.1.1"
    implementation("androidx.paging:paging-runtime-ktx:$pagingVersion")
}