plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.data"
    compileSdk = Versions.compileSdk
    buildToolsVersion = Versions.buildTools

    defaultConfig {
        minSdk = Versions.minSdk

        buildConfigField("int", "VERSION_CODE", Versions.appCode.toString())
        buildConfigField("String", "VERSION_NAME", "\"${Versions.appName}\"")

        consumerProguardFile("proguard-rules.pro")

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
        }
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
        sourceCompatibility = Versions.java
        targetCompatibility = Versions.java
    }
}

ktlint {
    version.set(Versions.ktlint)
    android.set(true)
    ignoreFailures.set(false)
}

dependencies {
    implementation(project(":preferences"))
    implementation(libs.androidx.paging)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.jellyfin.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
