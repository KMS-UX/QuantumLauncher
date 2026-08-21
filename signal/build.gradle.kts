plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// SIGNAL -- link diagnostics (cellular/wifi/GPS/Bluetooth gauges + signal sparkline + RUN SCAN),
// the last of the eight core instruments (SIGNAL + CONFIG Task Brief). Built natively in this
// monorepo from the first commit -- no standalone AI-Studio repo, no audit pass needed (brief §0).
// Depends on :app-shell for the shared chrome/tokens/fonts/SegmentedGauge, same shape as
// :comms/:files/:audio/:radio. RADIO listens (content comes in); SIGNAL measures (your own link,
// out) -- decision 60's locked split. No network dependency, no INTERNET permission.
android {
    namespace = "com.quantumos.signal"
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
    implementation(project(":quark-brain"))

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

    // Runtime permission flow (READ_PHONE_STATE / ACCESS_FINE_LOCATION / BLUETOOTH_CONNECT) --
    // same pin as :audio's RECORD_AUDIO flow and :optics's camera flow.
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    testImplementation("junit:junit:4.13.2")
}
