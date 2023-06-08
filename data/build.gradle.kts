plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "dev.jdtech.jellyfin.data"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    defaultConfig {
        minSdk = 27

        val appVersionCode: Int by rootProject.extra
        val appVersionName: String by rootProject.extra
        buildConfigField("int", "VERSION_CODE", appVersionCode.toString())
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")

        consumerProguardFile("proguard-rules.pro")

        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
            }
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
    implementation(project(":preferences"))
    implementation(libs.androidx.paging)
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.jellyfin.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.timber)
}
