plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Optics ("Blackhole") -- phosphor viewfinder + film emulation, ported from the standalone
// kms-ux/quantumoptics-blackhole repo (read-only source, never pushed to) and docked as a library
// module bundled into the single :app APK (App Shell Integration, Phase 3) rather than a separate
// installable application. Depends on :app-shell for the shared chrome/tokens/fonts instead of
// duplicating them (the standalone repo's own local Phosphor/Theme/CRT-shader/GoogleFont-provider
// are all dropped here).
android {
    namespace = "com.quantumos.optics"
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
    implementation(project(":app-shell"))

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Camera: preview + capture, matching the standalone repo's pinned versions.
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // Thumbnail/preview image loading (developing-console thumbnails).
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Spool log persistence (Room). Bumped 2.6.1 -> 2.7.1 from the standalone repo's pin: 2.6.1's
    // KSP integration (androidx.room.compiler.processing.ksp) throws
    // "IllegalStateException: unexpected jvm signature V" under this project's K2/KSP2 toolchain
    // (KSP 2.2.21-2.0.5) -- a known Room/KSP2 XProcessing bug fixed in later Room releases.
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    testImplementation("junit:junit:4.13.2")
}
