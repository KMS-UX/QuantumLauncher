plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// COMMS: field comms callsign channels + transmission threads, ported from the standalone
// QuantumComms repo (read-only source, never pushed to) and docked as a library module bundled
// into the single :app APK (Core Apps Fix-Pass, Decision 86) rather than a separate installable
// application. Depends on :app-shell for the shared chrome/tokens/fonts instead of duplicating
// them (the standalone repo's own local Color/Theme/Type.kt are all dropped here). COMMS has no
// camera/Room/network needs once its Gemini backend is stripped -- no KSP, no INTERNET permission.
android {
    namespace = "com.quantumos.comms"
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

    testImplementation("junit:junit:4.13.2")
}
