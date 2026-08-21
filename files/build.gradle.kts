plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// FILES ("field file manager") -- ported from the standalone kms-ux/quantumfiles repo (read-only
// source, never pushed to) and docked as a library module bundled into the single :app APK (Core
// Apps Fix-Pass, Decision 86) rather than a separate installable application. Depends on :app-shell
// for the shared chrome/tokens/fonts instead of duplicating them (the standalone repo's own local
// Color/Theme/Type are dropped here), and on :quark-brain for the real AiAssistBridge (QUARK Brain
// Promotion, Decision 88) that replaces the standalone repo's direct Gemini network calls -- QUARK's
// on-device brain now answers here, not a live cloud key. Plain java.io.File I/O (no Room), and
// no network deps of its own (Gemini stripped; the on-device brain needs none either) -- so this
// module stays deliberately thin next to :optics.
android {
    namespace = "com.quantumos.files"
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
    // QUARK Brain Promotion (decision 88): the real AiAssistBridge (QuarkBrainProvider.bridge) now
    // lives in :quark-brain, which depends only on :core — no circular dependency, since :app is the
    // one that depends on :files, never the reverse.
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

    testImplementation("junit:junit:4.13.2")
}
