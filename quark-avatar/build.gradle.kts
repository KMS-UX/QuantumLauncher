plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// QUARK AVATAR -- the parallel QUARK avatar art-production track (see
// art/quark-avatar/PRODUCTION_LOG.md; NOT part of the Tree 1.5 launcher milestone tree). Presents
// QUARK as NATIVE ART: the chroma-keyed hologram plates shown as the art they are, in a Compose
// projection housing, with four expression states and a materialise transition.
//
// NO 3D DEPENDENCY. SceneView/Filament was here for the DA3 relief and went with it in Phase 18 --
// 24.98 MB of native libraries across four ABIs for a path the Director cut. Everything this module
// draws is now Compose: a Canvas housing over an Image.
//
// Depends on :app-shell for the shared chrome/tokens/PhosphorHueRuntime, same shape as
// :signal/:comms/:files/:audio/:radio. No :quark-brain dependency (no live brain/state wiring this
// pass -- see PRODUCTION_LOG's non-goals) and no runtime permissions (static bundled art only).
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

    testImplementation("junit:junit:4.13.2")
}
