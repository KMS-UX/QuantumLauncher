plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Shared App Shell chrome (Phosphor tokens, bundled fonts, CRT container/shader, nameplate header,
// channel strip, PLEASE STANDBY beat) — every docked module (:optics, :nav, the launcher itself)
// depends on this so there is one source of the house-style chrome, not N reimplementations.
android {
    namespace = "com.quantumos.appshell"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        // Migrated from android { kotlinOptions { jvmTarget = "17" } }, which Kotlin 2.4
        // turned from a deprecation into a hard error.
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // Window/insets control for engageFieldUnitDisplay(), and the Activity extension it hangs off.
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // SoundEngine's synthesis runs off the main thread.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    testImplementation("junit:junit:4.13.2")
}
