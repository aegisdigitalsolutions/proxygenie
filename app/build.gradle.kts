plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.lanenode"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.lanenode"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Defaults injected from GitHub Secrets at build time.
        // Empty locally, so nothing sensitive ever lives in the repo.
        buildConfigField("String", "UPSTREAM_HOST",
            "\"${project.findProperty("UPSTREAM_HOST") ?: ""}\"")
        buildConfigField("int", "UPSTREAM_PORT",
            "${project.findProperty("UPSTREAM_PORT") ?: 20027}")
        buildConfigField("String", "UPSTREAM_USER",
            "\"${project.findProperty("UPSTREAM_USER") ?: ""}\"")
        buildConfigField("String", "UPSTREAM_PASS",
            "\"${project.findProperty("UPSTREAM_PASS") ?: ""}\"")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.02"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
}
