plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CONFIG -- the single settings home (SIGNAL + CONFIG Task Brief §3): Phosphor / Boot Pace /
// Deployment Region, all durable via the shared :app-shell SettingsStore (the same store the
// launcher itself reads/writes, so there is one source of truth, not two). No network dependency.
android {
    namespace = "com.quantumos.config"
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
    // The dev-preview row launches :quark-avatar's Activity by setClassName (no compile-time class
    // reference) to keep call-site coupling minimal, but Android library modules only end up in the
    // final APK's manifest/classes if something actually depends on them -- setClassName alone can't
    // conjure an Activity into the merged manifest. This is that real Gradle dependency edge.
    implementation(project(":quark-avatar"))

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
