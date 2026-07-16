plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// AUDIO -- field recorder first, player second (live waveform + REC timer), ported from the
// standalone kms-ux/QuantumAudio repo (read-only source, never pushed to) and docked as a library
// module bundled into the single :app APK (Core Apps Fix-Pass, Decision 86) rather than a separate
// installable application. Depends on :app-shell for the shared chrome/tokens/fonts instead of
// duplicating them (the standalone repo's own local AppShell/Color/Theme/Type are all dropped here).
// Recording/playback uses plain android.media.MediaRecorder/MediaPlayer -- no extra library needed --
// writing to app-internal filesDir. No Room, no network, no INTERNET permission.
android {
    namespace = "com.quantumos.audio"
    compileSdk = 35

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
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
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Runtime RECORD_AUDIO permission flow (mic capture) -- same pin as :optics's camera permission
    // flow, matching the standalone repo's own dependency version.
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    testImplementation("junit:junit:4.13.2")
}
