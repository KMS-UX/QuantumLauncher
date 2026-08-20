plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// QUARK AVATAR -- Phase 4b of the parallel QUARK 3D avatar art-production track (see
// art/quark-avatar/PRODUCTION_LOG.md; NOT part of the Tree 1.5 launcher milestone tree). Hosts the
// real-time AGSL overlay shader (rim glow + live hue-tinted emissive accent + Stealth dim) applied
// over the pre-rendered Blender posture-library frames, plus a dev-preview screen to see it on
// device. Depends on :app-shell for the shared chrome/tokens/PhosphorHueRuntime, same shape as
// :signal/:comms/:files/:audio/:radio. No :quark-brain dependency (no live brain/posture wiring
// this pass -- see PRODUCTION_LOG's non-goals) and no runtime permissions (static bundled art only).
android {
    namespace = "com.quantumos.quarkavatar"
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

    testImplementation("junit:junit:4.13.2")
}
