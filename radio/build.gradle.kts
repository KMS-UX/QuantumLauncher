plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// RADIO -- broadcast receiver (FM/AM/WX bands, presets, tuning dial, reception meter), ported from
// the standalone kms-ux/quantumradioreceiver repo (read-only source, never pushed to) and docked as a
// library module bundled into the single :app APK (Core Apps Fix-Pass, Decision 86) rather than a
// separate installable application. Depends on :app-shell for the shared chrome/tokens/fonts instead
// of duplicating them (the standalone repo's own local header/CRT-flicker/hardcoded-hex-color code is
// dropped here). RADIO is a pure content-receiver -- content comes IN -- so it carries no network
// dependency and no INTERNET permission: the standalone app's cryptographic signal-decoder feature
// (which called a live Gemini API) was deleted per the RADIO-listens / SIGNAL-measures split; see
// docs/future-signal/radio-decoder.md for where that code still lives.
android {
    namespace = "com.quantumos.radio"
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    testImplementation("junit:junit:4.13.2")
}
