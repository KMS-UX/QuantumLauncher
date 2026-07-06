plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// App Shell Integration Step 2 — Optics ("Blackhole"), docked as a module alongside the launcher.
// A copy of the standalone com.example/"My Application" project (kept untouched at its own repo
// as the rollback path — see the App Shell Integration Task Brief §0), repackaged to
// com.quantumos.optics and rebuilt on this repo's pinned toolchain (AGP/Kotlin/compileSdk/JVM all
// match :app and :app-shell) instead of the standalone project's own newer/unpinned versions.
android {
    namespace = "com.quantumos.optics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quantumos.optics"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
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
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.13.1")

    // Camera preview + capture (the primary function this app exists for).
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Runtime camera-permission prompt (the viewfinder's "GRANT ACCESS INTENT" gate).
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // Thumbnail loading for the spool/developing-console gallery.
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Spool log (past captures) — real Room persistence, unchanged from the standalone app.
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation(kotlin("test"))
}
