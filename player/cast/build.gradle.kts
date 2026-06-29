plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.jdtech.jellyfin.player.cast"
    compileSdk = Versions.COMPILE_SDK

    defaultConfig { minSdk = Versions.MIN_SDK }

    compileOptions {
        sourceCompatibility = Versions.JAVA
        targetCompatibility = Versions.JAVA
    }

    flavorDimensions += "variant"
    productFlavors {
        register("libre") { dimension = "variant" }
        register("proprietary") {
            dimension = "variant"
            isDefault = true
            matchingFallbacks += listOf("libre")
        }
    }
}

dependencies {
    implementation(projects.core)
    implementation(projects.data)
    implementation(projects.player.core)
    implementation(projects.settings)
    implementation(libs.androidx.lifecycle.viewmodel)
    api(libs.androidx.mediarouter)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.timber)

    "proprietaryImplementation"(libs.play.services.cast.framework)
}
